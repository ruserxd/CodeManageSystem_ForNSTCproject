# 實驗室機器部署設定（一次性）

push 到 `main` 且 `Run Tests` 成功後，實驗室機器上的 self-hosted runner 會執行 `.github/workflows/deploy.yml`：備份資料庫、登入 ghcr.io、`docker compose pull`、`up -d`、煙霧測試。本文件說明實驗室機器要事先完成的一次性設定。

實驗室在校園網路內，GitHub 連不進來。runner 由實驗室機器主動對外連線到 GitHub，所以**不用開放任何對外 port**。使用者瀏覽網頁的入口是 Cloudflare Tunnel：compose 內的 `cloudflared` 也是由實驗室機器主動往外連到 Cloudflare，HTTPS 在 Cloudflare 終止，再經 compose 內網轉給 nginx。nginx 的 80 只綁在 `127.0.0.1`（給本機煙霧測試用），校內其他機器連不進來。一次性設定見第 4.5 節。

下列固定值與 `deploy.yml` 一致，改動時兩邊要一起改：

| 項目 | 值 |
|---|---|
| runner label | `self-hosted`、`linux`、`x64`、`lab` |
| `.env` 路徑 | `/opt/codemanager/env/.env` |
| tunnel token 檔路徑 | `/opt/codemanager/env/cloudflared.env`（compose 的 profile 名稱是 `tunnel`） |
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
   ./config.sh --url https://github.com/ruserxd/CodeManageSystem_ForNSTCproject --token <頁面顯示的一次性註冊碼> --labels lab --name lab-runner
   ```

   `--name` 建議用中性名稱（例如 `lab-runner`）。不加的話預設是主機名稱，而公開 repo 的 Actions log 任何人都看得到 runner 名稱，主機名稱常帶有個人姓名。注意 log 另外還會顯示 `Machine name`，那是作業系統的 hostname，`--name` 改不到；要改得用 `hostnamectl`，共用機器請先問過管理者。

   已經註冊的 runner 要改名（在 runner 目錄 `/home/gh-runner/actions-runner` 執行；管理帳號 `cd` 被拒時見第 3 步最後的替代做法）：

   ```bash
   sudo ./svc.sh stop && sudo ./svc.sh uninstall                  # 管理帳號
   sudo -u gh-runner ./config.sh remove --token <移除碼>          # 以 gh-runner 執行；config.sh 拒絕以 root 執行
   sudo -u gh-runner ./config.sh --url <repo URL> --token <註冊碼> --labels lab --name lab-runner --unattended
   ```

   移除碼在 Runners 頁面點該 runner → Remove 取得；之後照第 3 步重新安裝服務。改名只影響之後的 run，舊 run 的 log 仍是舊名稱，要清除只能刪掉那些 run。
3. 安裝成開機自動啟動的服務。`gh-runner` **沒有也不該有** sudo 權限（以它執行 `sudo` 會被拒絕），所以這一步要先 `exit` 回到有 sudo 的管理帳號，在 runner 目錄執行。最後的 `gh-runner` 參數指定服務以哪個帳號執行：

   ```bash
   cd /home/gh-runner/actions-runner
   sudo ./svc.sh install gh-runner
   sudo ./svc.sh start
   sudo ./svc.sh status                          # 要看到 active (running)
   ps -o user=,pid= -C Runner.Listener           # 第一欄應為 gh-runner；沒有輸出表示 runner 沒在跑
   ```

   新版 Ubuntu 的 `useradd -m` 可能把家目錄建成 `0750`，管理帳號 `cd` 會被拒絕。這時改用 `sudo bash -c 'cd /home/gh-runner/actions-runner && ./svc.sh install gh-runner && ./svc.sh start'`。

   runner 的 log：`journalctl -u 'actions.runner.*' -f`。
4. 回到 Runners 頁面，確認 runner 狀態為 Idle（不是 Offline），且 labels 包含 `lab`。只註冊、還沒 `start` 時會顯示 Offline。

## 4. 放置 `.env`

`.env` 只存在實驗室機器上，**不進 git、不進映像**。放在 runner 工作目錄之外（checkout 會清掉工作目錄內的檔案），deploy 時 workflow 會在工作目錄建一個指向它的符號連結。

以下全部在**有 sudo 的管理帳號**下執行，不用切換到 `gh-runner`。寫 `.env` 時用 `sudo -u gh-runner` 暫時借用它的身分，檔案的擁有者才會是 `gh-runner`。不要用 `sudo nano` 直接編輯，那樣檔案會變成 root 擁有，runner 讀不到。

```bash
sudo mkdir -p /opt/codemanager/env /opt/codemanager/backup
sudo chown -R gh-runner:gh-runner /opt/codemanager
sudo chmod 700 /opt/codemanager/env /opt/codemanager/backup
# 以 gh-runner 身分建立並編輯，欄位依 .env.example
sudo -u gh-runner nano /opt/codemanager/env/.env
sudo -u gh-runner chmod 600 /opt/codemanager/env/.env
sudo ls -l /opt/codemanager/env/.env     # 預期 -rw------- gh-runner gh-runner
```

`chmod 700` 之後，管理帳號自己執行 `ls` 也會出現 `Permission denied`，這是預期結果，表示只有 `gh-runner` 進得去。要查看時請加 `sudo`。

欄位請參照 repo 根目錄的 `.env.example`，部署至少要填 `JWT_SECRET`（至少 32 字元）、`DOCKER_DATASOURCE_URL`、`DOCKER_DATASOURCE_USERNAME`、`DOCKER_DATASOURCE_PASSWORD`。**不要照抄 `.env.example` 的佔位值**（#76），有兩個值必須和 `docker-compose.yml` 的 `db` service 一致：

| 欄位 | 必須是 | 原因 |
|---|---|---|
| `DOCKER_DATASOURCE_URL` | `jdbc:mysql://db:3306/myjdbc?...`（`?` 之後的參數照 `.env.example`） | 主機名稱是 compose service 名稱 `db`（不是 `localhost`），資料庫名稱是 `MYSQL_DATABASE=myjdbc` |
| `DOCKER_DATASOURCE_USERNAME` | `root` | compose 只設定了 `MYSQL_ROOT_PASSWORD`，沒有建立其他帳號 |

`JWT_SECRET` 可用 `openssl rand -base64 48` 產生。`APP_ADMIN_ACCOUNT`／`APP_ADMIN_PASSWORD` 兩個都留空就不會建立管理員。

`.env` 裡的 `SPRING_DATASOURCE_*` 是本機開發用的，部署時不會生效：compose 的 `environment:` 會用 `DOCKER_DATASOURCE_*` 覆蓋它們（`environment:` 優先於 `env_file`）。

- `IMAGE_TAG`（`.env.example` 預設是 `latest`）保留或刪除都可以：workflow 會以環境變數（commit SHA 或手動輸入的 tag）覆蓋它，`.env` 裡的值不會生效。要回退請用第 7 節的方式。
- `VITE_API_URL` 維持空字串（同源）。這個值是 build 映像時寫入的，部署只 pull 已建好的映像，改了不會生效。
- `DOCKER_DATASOURCE_PASSWORD` 同時是 MySQL root 的初始密碼，只在 `mysql_data` volume 第一次建立時生效，之後再改 `.env` 不會改到資料庫裡的密碼。

填完後可用下面的指令自我檢查。名稱含 `PASSWORD`、`SECRET`、`TOKEN`、`KEY` 的欄位只顯示長度（已扣掉前後的雙引號，與 compose 實際讀到的值一致；值請不要用單引號包起來）：

```bash
sudo -u gh-runner awk -F= '
  /^#/||NF<2 {next}
  $1 ~ /PASSWORD|SECRET|TOKEN|KEY/ {v=substr($0, index($0,"=")+1); gsub(/^"|"$/, "", v); print $1 " = (len " length(v) ")"; next}
  {print}' /opt/codemanager/env/.env
```

預期：`JWT_SECRET` 長度 ≥ 32，URL 含 `db:3306/myjdbc`，`DOCKER_DATASOURCE_USERNAME=root`。其他欄位（例如 `APP_ADMIN_ACCOUNT` 的管理員帳號名稱）會原樣印出，**輸出貼到公開的地方（issue、PR、Actions log）之前要先遮掉**。

## 4.5 設定 Cloudflare Tunnel（HTTPS 對外網址）

nginx 只綁 `127.0.0.1:80`，外部使用者要透過 Cloudflare Tunnel 才連得到網站。**這一節要在 PR 合併前完成**：`deploy.yml` 找不到 `/opt/codemanager/env/cloudflared.env` 會直接失敗，不會靜默略過（沒有 tunnel 就等於誰都連不到網站，靜默略過會讓人以為部署成功）。

**對外開放前**，先確認學校或計中對於把校內服務放上外網的規範。

### 4.5.1 Cloudflare 帳號與網域

1. 申請 Cloudflare 帳號。
2. 取得一個網域，並把 DNS 交給 Cloudflare 管理（在網域註冊商把 nameserver 改成 Cloudflare 指定的值）。

### 4.5.2 建立 tunnel 並取得 token

1. 到 Cloudflare Zero Trust 後台：Networks -> Tunnels -> Create a tunnel，類型選 **Cloudflared**（remotely-managed）。
2. 名稱自訂。建立後頁面會顯示安裝指令，其中 `--token` 後面那一長串就是 tunnel token。**只複製 token，不要在機器上執行頁面給的安裝指令**（我們用 compose 跑 `cloudflared`）。

### 4.5.3 Public Hostname

在該 tunnel 的 Public Hostname 新增一筆：

| 欄位 | 值 |
|---|---|
| Subdomain／Domain | 自訂，例如 `codemanager.<你的網域>` |
| Service type | `HTTP` |
| URL | `nginx:80`（即 `http://nginx:80`，`nginx` 是 compose service 名稱） |

前端與 API 都走同源（nginx 轉發），所以經過 Cloudflare 後不會有 CORS 問題。

### 4.5.4 Cloudflare Access（限定 email）

**預設要開啟 Access。** 原因：`/api/register` 是公開的，而 repo 大小上限還沒做（P1-10），陌生人可以註冊後讓實驗室機器去 clone 很大的 repo。P1-10 完成後再評估是否公開。

Zero Trust 後台：Access -> Applications -> Add an application -> **Self-hosted**：

1. Application domain 填上一節的 hostname（要涵蓋整個 hostname）。
2. 新增 policy，Action 選 **Allow**，Include 規則選 Emails，填允許進入的 email。
3. 儲存。

### 4.5.5 放置 `cloudflared.env`

token 放**獨立的檔案**，不要放進 `.env`。原因：backend 在 compose 用 `env_file: .env`，會把 `.env` 的所有變數載入 backend 容器；backend 是處理使用者輸入、還會 clone 外部 repo 的服務，不該拿到 tunnel 憑證。`cloudflared` 則只讀 `cloudflared.env`。

在**有 sudo 的管理帳號**下執行，做法同第 4 節（用 `sudo -u gh-runner` 寫入，檔案擁有者才會是 `gh-runner`）：

```bash
# 以 gh-runner 身分建立，內容只有一行：TUNNEL_TOKEN=<token>
sudo -u gh-runner nano /opt/codemanager/env/cloudflared.env
sudo -u gh-runner chmod 600 /opt/codemanager/env/cloudflared.env
sudo ls -l /opt/codemanager/env/cloudflared.env     # 預期 -rw------- gh-runner gh-runner
```

自我檢查，只顯示欄位名稱與長度，不印出 token 值：

```bash
sudo -u gh-runner awk -F= '
  /^#/||NF<2 {next}
  {v=substr($0, index($0,"=")+1); gsub(/^"|"$/, "", v); print $1 " = (len " length(v) ")"}' /opt/codemanager/env/cloudflared.env
```

預期只有一行 `TUNNEL_TOKEN = (len N)`，N 大於 0（token 通常上百字元）。

### 4.5.6 風險與注意事項

- token 檔請放在 `/opt/codemanager/env/`，**不要在 repo 目錄放真 token**。`.gitignore` 已排除 `cloudflared.env`，但擋不住 `git add -f`，也擋不住別的檔名。
- token 以環境變數 `TUNNEL_TOKEN` 傳進容器，有 docker 權限的人可用 `docker inspect` 看到。`gh-runner` 本來就等同 root（見第 2 節），所以這沒有擴大風險面。
- token 洩漏時：到 Cloudflare 後台該 tunnel 做 Refresh／Roll token，更新 `cloudflared.env`，再手動觸發一次 Deploy（第 7 節）讓容器重建。
- TLS 在 Cloudflare 終止。`cloudflared` 到 nginx 這一段是 compose 內網的明文 HTTP，不經過校園網路。

### 4.5.7 部署後驗收

1. 看 `cloudflared` 的 log，應看到 tunnel 已註冊連線（Registered tunnel connection）：

   ```bash
   docker compose -p codemanager --profile tunnel logs cloudflared
   ```

   deploy 的煙霧測試只能確認 `cloudflared` 容器 15 秒內持續執行、沒有重啟，**確認不了 tunnel 已連上 Cloudflare**，所以這一步要自己看。
2. 從校外網路（例如手機行動網路）開 `https://<你的網域>`：應先出現 Cloudflare Access 登入頁，輸入允許名單內的 email 並完成驗證後，看得到網站。
3. 用不在允許名單的 email 嘗試，應被擋下。
4. 在實驗室機器確認 nginx 只綁本機：`ss -ltn | grep -E ':80\s'` 只應出現 `127.0.0.1:80`，沒有 `0.0.0.0:80`、`*:80`、`[::]:80`。從校內另一台機器 `curl --max-time 5 http://<實驗室機器 IP>/` 應連線被拒或逾時。

## 5. 設定外部貢獻者的 workflow 需核准

repo 是公開的，要避免外部 fork PR 自動跑 workflow。

Settings -> Actions -> General -> Fork pull request workflows from outside collaborators -> 選 **Require approval for all external contributors** -> Save。

核准 fork PR 的 workflow 之前，先檢查它有沒有修改 workflow 的 `runs-on` 或觸發條件，否則對方的程式碼可能在 `lab` runner 上執行。

deploy workflow 本身只由 `workflow_run`（main 上的 `Run Tests` 完成）與 `workflow_dispatch` 觸發，不會被 PR 觸發。

## 6. 第一次部署前：檢查機器、處理舊部署

### 6.0 所有機器都要先檢查：port 80 與其他 compose 專案

實驗室機器常常是共用的。不同 compose project 的網路、volume、容器名稱互不影響，但 **主機 port** 和 **映像快取** 是整台機器共用的。以下用管理帳號執行（不在 `docker` group 的話，`docker` 指令前加 `sudo`）：

```bash
docker compose ls -a                                  # 機器上有哪些 compose 專案
docker ps --format '{{.Names}}\t{{.Ports}}'           # 各容器佔用的 port
ss -ltn | grep -E ':80\s'                             # 有輸出 = port 80 已被佔用（要看是哪個程式再加 sudo 與 -p）。本專案的 nginx 綁的是 127.0.0.1:80，所以看到 127.0.0.1:80 也算被佔用
docker volume ls | grep backend_clonecode             # 有輸出 = 這台有本專案的舊部署
```

- port 80 被其他容器或主機上的服務（apache、nginx 等）佔用時，`up -d` 會失敗，要先停掉對方或改它的 port。
- 不需要停掉其他專案；真的要停時，建議用 `docker compose -p <專案> stop`，不要用 `down`。原因見「已知限制」的 `image prune`。
- 最後一行沒有輸出就是全新機器，可以跳過 6.1–6.3。`backend_clonecode` 是本專案特有的 volume 名稱；`mysql_data` 在共用機器上很常見，用它判斷容易誤判。

**6.1–6.3 只在實驗室機器上已經有舊的 compose 部署時需要**；全新機器沒有舊容器與 volume，跳過即可。

V2 migration 會在第一次部署時清空 Project 等 5 張表，而 `backend_clonecode` volume 裡舊的 clone 還在。若不清掉，加入同名 repo 時 `GitCloner` 會走 pull 路徑，去找已被清掉的 Project。

**手動指令的執行目錄**：6.1–6.3 與第 7 節所有 `docker compose` 指令，都要在放有 `docker-compose.yml` 的目錄（repo checkout，例如 runner 的工作目錄 `_work/<repo>/<repo>`，或你自己 clone 的一份）執行，並以 `gh-runner` 身分。該目錄要有 `.env`，沒有的話先建符號連結（和 workflow 做法相同）：

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
- 部署最後的 `docker image prune -af --filter "until=168h"` 是**整台機器範圍**：只要映像超過 7 天且沒有被任何容器使用，就會被刪，包含實驗室機器上其他專案的映像。若這台機器還有別的用途，請先確認。已停止（`stop`）的容器仍算「在使用」，它的映像不會被刪；`down` 會移除容器，映像就沒人使用。`until` 是以映像的**建立時間**計算，不是多久沒被使用，所以 `down` 之後，只要映像是 7 天前建立的，下一次部署就會立刻被清掉。
- 備份資料夾不會自動輪替清理，也沒有遠端備份，請定期手動整理。
- 公開 repo 的 Actions log 任何人都能看。重跑 workflow 時不要勾「Enable debug logging」，debug log 會多出大量內部路徑；真的需要除錯時，結束後把那次 run 刪掉（`gh run delete <run id>`）。secret 在 log 中會被遮成 `***`，但 secret 以外的輸出不會。
- runner 帳號等同 root（見第 2 節），runner 工作目錄不會自動清空，所以 workflow 以 `persist-credentials: false` 避免憑證留在硬碟上，並在結束時 `docker logout ghcr.io`。
