# 實驗室機器部署設定（一次性）

push 到 `main` 且 `Run Tests` 成功後，實驗室機器上的 self-hosted runner 會執行 `.github/workflows/deploy.yml`：備份資料庫、登入 ghcr.io、`docker compose pull`、`up -d`、煙霧測試。本文件說明實驗室機器要事先完成的一次性設定。

實驗室在校園網路內，GitHub 連不進來。runner 由實驗室機器主動對外連線到 GitHub，所以**不用開放任何對外 port**（對外只有 nginx 的 80，給使用者瀏覽網頁用）。

下列固定值與 `deploy.yml` 一致，改動時兩邊要一起改：

| 項目 | 值 |
|---|---|
| runner label | `self-hosted`、`linux`、`x64`、`lab` |
| `.env` 路徑 | `/opt/codemanager/env/.env` |
| 備份資料夾 | `/opt/codemanager/backup` |
| compose project name | `codemanager`（`docker compose -p codemanager`） |
| 手動部署的輸入名稱 | `images-tag`（job 內對應環境變數 `IMAGE_TAG`） |

## 1. 安裝 Docker

需要 Docker Engine 與 Compose v2 plugin（指令是 `docker compose`，不是 `docker-compose`）。以 Ubuntu 為例，依官方文件安裝：<https://docs.docker.com/engine/install/ubuntu/>

```bash
docker --version
docker compose version
sudo systemctl enable --now docker
```

## 2. 建立專用帳號並加入 docker group

```bash
sudo useradd -m -s /bin/bash gh-runner
sudo usermod -aG docker gh-runner
```

注意：加入 `docker` group 等同擁有這台機器的 root 權限（可以掛載 `/` 進容器）。所以請使用**專用帳號**，不要用個人帳號，也不要把這個帳號拿來做別的事。

## 3. 註冊 self-hosted runner

1. 到 GitHub repo：Settings -> Actions -> Runners -> New self-hosted runner，選 Linux / x64。
2. 以 `gh-runner` 登入，照頁面指示下載並執行 `config.sh`。註冊時的 labels 輸入 `lab`（`self-hosted`、`linux`、`x64` 會自動帶上）：

   ```bash
   ./config.sh --url https://github.com/ruserxd/CodeManageSystem_ForNSTCproject --token <頁面顯示的一次性註冊碼> --labels lab
   ```
3. 安裝成開機自動啟動的服務：

   ```bash
   sudo ./svc.sh install gh-runner
   sudo ./svc.sh start
   sudo ./svc.sh status
   ```
4. 回到 Runners 頁面，確認 runner 狀態為 Idle，且 labels 包含 `lab`。

## 4. 放置 `.env`

`.env` 只存在實驗室機器上，**不進 git、不進映像**。放在 runner 工作目錄之外（checkout 會清掉工作目錄內的檔案），deploy 時 workflow 會在工作目錄建一個指向它的符號連結。

```bash
sudo mkdir -p /opt/codemanager/env /opt/codemanager/backup
sudo chown -R gh-runner:gh-runner /opt/codemanager
sudo chmod 700 /opt/codemanager/env /opt/codemanager/backup
# 以 gh-runner 身分，依 .env.example 的欄位填入實際值
cp .env.example /opt/codemanager/env/.env     # 或自行建立
chmod 600 /opt/codemanager/env/.env
```

欄位請參照 repo 根目錄的 `.env.example`，部署至少要填 `JWT_SECRET`（至少 32 字元）、`DOCKER_DATASOURCE_URL`、`DOCKER_DATASOURCE_USERNAME`、`DOCKER_DATASOURCE_PASSWORD`。
- `IMAGE_TAG`（`.env.example` 預設是 `latest`）保留或刪除都可以：workflow 會以環境變數（commit SHA 或手動輸入的 tag）覆蓋它，`.env` 裡的值不會生效。要回退請用第 7 節的方式。
- `VITE_API_URL` 維持空字串（同源）。這個值是 build 映像時寫入的，部署只 pull 已建好的映像，改了不會生效。
- `DOCKER_DATASOURCE_PASSWORD` 同時是 MySQL root 的初始密碼，只在 `mysql_data` volume 第一次建立時生效，之後再改 `.env` 不會改到資料庫裡的密碼。

## 5. 設定外部貢獻者的 workflow 需核准

repo 是公開的，要避免外部 fork PR 自動跑 workflow。

Settings -> Actions -> General -> Fork pull request workflows from outside collaborators -> 選 **Require approval for all external contributors** -> Save。

核准 fork PR 的 workflow 之前，先檢查它有沒有修改 workflow 的 `runs-on` 或觸發條件，否則對方的程式碼可能在 `lab` runner 上執行。

deploy workflow 本身只由 `workflow_run`（main 上的 `Run Tests` 完成）與 `workflow_dispatch` 觸發，不會被 PR 觸發。

## 6. 第一次部署前：備份資料庫並清空 clone volume

V2 migration 會在第一次部署時清空 Project 等 5 張表，而 `backend_clonecode` volume 裡舊的 clone 還在。若不清掉，加入同名 repo 時 `GitCloner` 會走 pull 路徑，去找已被清掉的 Project。

以下只在**實驗室機器上已經有舊的 compose 部署**時需要；全新機器沒有舊容器與 volume，跳過即可。

**手動指令的執行目錄**：第 6、7 節所有 `docker compose` 指令，都要在放有 `docker-compose.yml` 的目錄（repo checkout，例如 runner 的工作目錄 `_work/<repo>/<repo>`，或你自己 clone 的一份）執行，並以 `gh-runner` 身分。該目錄要有 `.env`，沒有的話先建符號連結（和 workflow 做法相同）：

```bash
ln -sfn /opt/codemanager/env/.env .env
```

### 6.1 找出舊部署的 project name

`docker-compose.yml` 沒有寫頂層 `name:`，舊部署的 project name 預設取自**當時啟動目錄的名稱**，不一定是 `codemanager`。先查：

```bash
docker compose ls -a
```

`NAME` 欄就是 project name。記下它，下面以 `<OLD>` 表示。volume 名稱是 `<project name>_<volume 名稱>`，可用 `docker volume ls | grep -E "mysql_data|backend_clonecode"` 確認。

### 6.2 情況 A：`<OLD>` 就是 `codemanager`

```bash
# 1. 備份（密碼取自容器環境，不寫在命令列）
umask 077
docker compose -p codemanager exec -T db sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --databases "$MYSQL_DATABASE" --single-transaction --routines --triggers' \
  > /opt/codemanager/backup/db-manual-$(date +%Y%m%d-%H%M%S).sql

# 2. 先停掉並移除 backend 容器，volume 被容器使用時 docker volume rm 會失敗
docker compose -p codemanager stop backend
docker compose -p codemanager rm -f backend

# 3. 刪除 clone volume
docker volume rm codemanager_backend_clonecode
```

資料庫沿用原本的 `codemanager_mysql_data`，不用還原。接著到 GitHub Actions 手動觸發 Deploy（見第 7 節，`images-tag` 填 `latest`）。

### 6.3 情況 B：`<OLD>` 不是 `codemanager`

新部署會用 `codemanager` 建立全新的空資料庫 volume，舊 project 的 volume 不會被沿用；舊的 nginx 也還佔著 port 80，新部署會失敗。所以要先備份、停掉舊的，再把備份還原進新的資料庫。

```bash
# 1. 用舊 project name 備份
umask 077
docker compose -p <OLD> exec -T db sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --databases "$MYSQL_DATABASE" --single-transaction --routines --triggers' \
  > /opt/codemanager/backup/db-manual-$(date +%Y%m%d-%H%M%S).sql
ls -l /opt/codemanager/backup/      # 確認檔案不是空的

# 2. 停掉並移除舊 stack（釋放 port 80）。不要加 -v，舊的 mysql_data volume 先留著當第二份備份
docker compose -p <OLD> down

# 3. 刪除舊的 clone volume（down 之後已沒有容器使用它）
docker volume rm <OLD>_backend_clonecode

# 4. 只啟動新 project 的 db，等它 healthy
docker compose -p codemanager up -d db
docker compose -p codemanager ps db       # STATUS 出現 (healthy) 再繼續

# 5. 在 backend 啟動之前，把備份還原進新的 db
docker compose -p codemanager exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' \
  < /opt/codemanager/backup/<剛剛的備份檔>.sql
```

還原後到 GitHub Actions 手動觸發 Deploy（`images-tag` 填 `latest`）。backend 第一次啟動時才會跑 V2 migration，所以資料的處理與情況 A 相同。確認新部署正常運作（登入、查看專案）之後，再清掉舊資料：

```bash
docker volume rm <OLD>_mysql_data <OLD>_backend_log
```

注意：還原的是舊資料庫的內容，不會改變新 `codemanager` db 的 root 密碼（以 `/opt/codemanager/env/.env` 為準）。

## 7. 手動部署與回退

Actions -> Deploy -> Run workflow，欄位 `images-tag`：

- `latest`（預設）：部署最新映像。
- 40 碼完整 commit SHA：部署該次 CI 推上去的映像，用於回退到指定版本。

注意：手動執行時 checkout 的是所選分支的 HEAD，`images-tag` 是另外輸入的，兩者可能不是同一版（compose 檔與映像版本不一致）。回退時請確認 compose 檔沒有在這之間做過會影響執行的改動。

每次部署前，若 db 容器正在執行，會先備份到 `/opt/codemanager/backup/db-<時間>-<tag 前 7 碼>.sql`（這 7 碼是**即將部署**的版本，不是備份當下正在執行的版本），備份失敗就不部署。要還原：

```bash
docker compose -p codemanager exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < /opt/codemanager/backup/<檔名>.sql
```

## 已知限制

- `compose up -d` 重建容器時服務會短暫中斷。零停機部署要等之後換成 k3s。
- 部署最後的 `docker image prune -af --filter "until=168h"` 是**整台機器範圍**：只要映像超過 7 天且沒有被任何容器使用，就會被刪，包含實驗室機器上其他專案的映像。若這台機器還有別的用途，請先確認。
- 備份資料夾不會自動輪替清理，也沒有遠端備份，請定期手動整理。
- runner 帳號等同 root（見第 2 節），runner 工作目錄不會自動清空，所以 workflow 以 `persist-credentials: false` 避免憑證留在硬碟上，並在結束時 `docker logout ghcr.io`。
