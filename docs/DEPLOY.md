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
- 不要在這份 `.env` 填 `IMAGE_TAG`：workflow 會以 commit SHA（或手動輸入的 tag）設定它，要回退請用第 7 節的方式。
- `VITE_API_URL` 維持空字串（同源）。這個值是 build 映像時寫入的，部署只 pull 已建好的映像，改了不會生效。
- `DOCKER_DATASOURCE_PASSWORD` 同時是 MySQL root 的初始密碼，只在 `mysql_data` volume 第一次建立時生效，之後再改 `.env` 不會改到資料庫裡的密碼。

## 5. 設定外部貢獻者的 workflow 需核准

repo 是公開的，要避免外部 fork PR 自動跑 workflow。

Settings -> Actions -> General -> Fork pull request workflows from outside collaborators -> 選 **Require approval for all outside collaborators**（保守的話選 all external contributors）-> Save。

deploy workflow 本身只由 `workflow_run`（main 上的 `Run Tests` 完成）與 `workflow_dispatch` 觸發，不會被 PR 觸發。

## 6. 第一次部署前：備份資料庫並清空 clone volume

V2 migration 會在第一次部署時清空 Project 等 5 張表，而 `backend_clonecode` volume 裡舊的 clone 還在。若不清掉，加入同名 repo 時 `GitCloner` 會走 pull 路徑，去找已被清掉的 Project。

以下只在**已經有舊的 compose 部署**時需要；全新機器沒有舊 volume，跳過即可。以 `gh-runner` 身分，在放有 `docker-compose.yml` 的目錄執行（`-p codemanager` 一定要加，volume 名稱才會對得上）。

```bash
# 1. 備份（檔名與 workflow 的備份相同格式；密碼取自容器環境，不寫在命令列）
umask 077
docker compose -p codemanager exec -T db sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --databases "$MYSQL_DATABASE" --single-transaction --routines --triggers' \
  > /opt/codemanager/backup/db-manual-$(date +%Y%m%d-%H%M%S).sql

# 2. 先停掉 backend，volume 被容器使用時 docker volume rm 會失敗
docker compose -p codemanager stop backend

# 3. 刪除 clone volume（名稱 = <project name>_<volume 名稱>）
docker compose -p codemanager rm -f backend
docker volume rm codemanager_backend_clonecode
```

完成後到 GitHub Actions 頁面，手動觸發 Deploy（見第 7 節，tag 填 `latest`）。部署時 compose 會重建 backend 與空的 `codemanager_backend_clonecode`。

## 7. 手動部署與回退

Actions -> Deploy -> Run workflow，欄位 `images-tag`：

- `latest`（預設）：部署最新映像。
- 40 碼完整 commit SHA：部署該次 CI 推上去的映像，用於回退到指定版本。

注意：手動執行時 checkout 的是所選分支的 HEAD，`images-tag` 是另外輸入的，兩者可能不是同一版（compose 檔與映像版本不一致）。回退時請確認 compose 檔沒有在這之間做過會影響執行的改動。

每次部署前，若 db 容器正在執行，會先備份到 `/opt/codemanager/backup/db-<時間>-<tag 前 7 碼>.sql`，備份失敗就不部署。要還原：

```bash
docker compose -p codemanager exec -T db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < /opt/codemanager/backup/<檔名>.sql
```

## 已知限制

- `compose up -d` 重建容器時服務會短暫中斷。零停機部署要等之後換成 k3s。
- 部署最後的 `docker image prune -af --filter "until=168h"` 是**整台機器範圍**：只要映像超過 7 天且沒有被任何容器使用，就會被刪，包含實驗室機器上其他專案的映像。若這台機器還有別的用途，請先確認。
- 備份資料夾不會自動輪替清理，也沒有遠端備份，請定期手動整理。
- runner 帳號等同 root（見第 2 節），runner 工作目錄不會自動清空，所以 workflow 以 `persist-credentials: false` 避免憑證留在硬碟上，並在結束時 `docker logout ghcr.io`。
