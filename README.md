# АвтоСервис Учёт — Пошаговая инструкция для новичка

Мобильное приложение для мастеров автосервиса:
- Фотографирование детали с автоматическим сжатием (**длинная сторона до 1280 px, JPEG ~0.8**).
- Ввод номера детали и выбор вида ремонта (`замена`, `ремонт`, `диагностика`, `покраска`, `сварка`, `регулировка` + сохранение своих вариантов).
- Автоматическая подстановка даты и времени (`ДД.ММ.ГГГГ ЧЧ:ММ`) с возможностью ручного редактирования.
- Полноэкранный просмотр фотографии с приближением (Pinch-to-Zoom / двойной тап).
- Полностью локальное хранение без интернета + выгрузка таблицы в **CSV** (с поддержкой русских символов в Excel).
- Автоматическая проверка обновлений через **GitHub Releases API** и скачивание `.apk` с цветной шкалой прогресса.

---

## ЧАСТЬ 1. Как открыть проект в Android Studio и собрать APK

В этом проекте уже настроена готовая Android-сборка, а также приложен файл `update/updateManager.ts` для гибридной обёртки Capacitor. Ниже описаны оба варианта (прямая сборка в Android Studio и сборка через Capacitor).

### Вариант А: Прямая сборка готового Android-проекта в Android Studio
1. Скачайте архив проекта (кнопка **Export / Download ZIP**) и распакуйте его в удобную папку.
2. Откройте **Android Studio** -> нажмите **Open** -> выберите папку проекта.
3. Дождитесь окончания синхронизации Gradle (внизу справа полоска `Gradle Sync`).
4. Чтобы собрать установочный файл `.apk`:
   - В верхнем меню выберите **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
   - Когда внизу появится уведомление `Build APK(s): APK(s) generated successfully`, нажмите **locate**.
   - Готовый файл лежит в `app/build/outputs/apk/debug/app-debug.apk`.

### Вариант Б: Сборка веб-приложения (React + Vite + TypeScript + Tailwind) через Capacitor в APK
Если вы собираете веб-проект через **Capacitor**, выполните по шагам в терминале (в корне папки веб-проекта):

1. **Установите зависимости проекта и плагины Capacitor:**
   ```bash
   npm install
   npm install @capacitor/core @capacitor/cli @capacitor/android @capacitor/camera @capacitor/filesystem @capacitor-community/file-opener
   ```
2. **Инициализируйте конфигурацию Capacitor (если ещё не создана):**
   ```bash
   npx cap init "АвтоСервис Учёт" com.aistudio.autoservice.kxmpzq --web-dir dist
   ```
3. **Соберите веб-приложение (Vite) и добавьте платформу Android:**
   ```bash
   npm run build
   npx cap add android
   npx cap sync
   ```
4. **Проверьте разрешения для камеры и установки APK в `android/app/src/main/AndroidManifest.xml`:**
   Откройте файл `AndroidManifest.xml` и убедитесь, что внутри тега `<manifest>` (перед `<application>`) прописаны следующие разрешения:
   ```xml
   <!-- Интернет для проверки обновлений на GitHub -->
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

   <!-- Доступ к камере для фотографирования деталей -->
   <uses-permission android:name="android.permission.CAMERA" />

   <!-- Разрешение на установку скачанного APK при автообновлении с GitHub -->
   <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
   ```
5. **Откройте проект в Android Studio и соберите APK:**
   ```bash
   npx cap open android
   ```
   В открывшемся окне Android Studio выберите **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.

---

## ЧАСТЬ 2. Как загрузить проект на GitHub

1. Зарегистрируйтесь или войдите на [https://github.com](https://github.com).
2. Нажмите кнопку **«+»** в правом верхнем углу -> **New repository**:
   - **Repository name**: например, `autoservice-app`
   - Выберите **Public** (чтобы GitHub API для проверки релизов работал без токенов авторизации).
   - Нажмите **Create repository**.
3. Откройте терминал в папке вашего проекта и выполните:
   ```bash
   git init
   git add .
   git commit -m "Первый коммит: АвтоСервис Учёт v1.0.0"
   git branch -M main
   git remote add origin https://github.com/ВАШ_ЛОГИН/autoservice-app.git
   git push -u origin main
   ```

---

## ЧАСТЬ 3. Как настроить автоматическую сборку APK через GitHub Actions

В проекте уже создан готовый файл сценария:
`/.github/workflows/release-apk.yml`

Что он делает автоматически:
1. Срабатывает каждый раз, когда вы отправляете на GitHub новый тег версии, начинающийся с буквы `v` (например, `v1.0.1`).
2. Устанавливает Java 17 и запускает сборку APK (`assembleDebug`).
3. Переименовывает готовый файл в `autoservice-1.0.1.apk`.
4. Создаёт релиз в разделе **Releases** вашего репозитория и прикрепляет `.apk` в `assets` релиза.

Чтобы дать GitHub Actions право публиковать релизы:
1. На странице вашего репозитория на GitHub откройте вкладку **Settings** -> в меню слева выберите **Actions** -> **General**.
2. Прокрутите вниз до блока **Workflow permissions**.
3. Выберите пункт **Read and write permissions** и нажмите **Save**.

---

## ЧАСТЬ 4. Как выпустить новую версию, чтобы приложение у мастеров показало обновление

1. **Укажите свой репозиторий в коде (один раз перед первой сборкой):**
   - В файле `app/src/main/java/com/example/update/UpdateManager.kt` (и/или `update/updateManager.ts`) поменяйте константу:
     ```kotlin
     const val GITHUB_REPO = "ВАШ_ЛОГИН/autoservice-app"
     ```
2. **Когда нужно выпустить новую версию (например, `1.0.1`):**
   - Поменяйте константу `APP_VERSION` в `UpdateManager.kt` (и `update/updateManager.ts`):
     ```kotlin
     const val APP_VERSION = "1.0.1"
     ```
   - Зафиксируйте изменения и отправьте их в GitHub вместе с тегом `v1.0.1`:
     ```bash
     git add .
     git commit -m "Релиз версии 1.0.1: описание изменений"
     git push origin main

     git tag v1.0.1
     git push origin v1.0.1
     ```
3. **Дождитесь окончания сборки в GitHub Actions (обычно 2–3 минуты):**
   - Перейдите во вкладку **Actions** на GitHub и дождитесь зелёной галочки.
   - Во вкладке **Releases** появится релиз `v1.0.1` с прикреплённым файлом `autoservice-1.0.1.apk` (при желании отредактируйте текст описания релиза `body` — именно его увидят мастера в приложении).
4. **Что произойдёт на телефонах у мастеров (у которых установлена версия `1.0.0`):**
   - При следующем запуске приложения (или при нажатии кнопки **«Проверить обновление»** в разделе «О приложении») приложение запросит `https://api.github.com/repos/ВАШ_ЛОГИН/autoservice-app/releases/latest`.
   - Сравнит числа версии `1.0.1 > 1.0.0` и покажет заметный баннер: **«Доступно обновление: 1.0.1»** со списком изменений.
   - По нажатию **«Скачать и установить»** скачает `.apk` с зелёной шкалой прогресса и откроет системное окно установки обновления поверх текущей версии (все локальные записи и фотографии сохранятся!).
