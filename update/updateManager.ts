/**
 * Модуль автообновления через GitHub Releases (для веб/Capacitor сборки).
 * Нативная Android-реализация находится в:
 * app/src/main/java/com/example/update/UpdateManager.kt
 */

export const APP_VERSION = "1.0.0";
export const GITHUB_REPO = "vovik06302-ai/avtoServis";

export interface GitHubReleaseAsset {
  name: string;
  browser_download_url: string;
  size: number;
}

export interface GitHubReleaseResponse {
  tag_name: string;
  body: string;
  html_url: string;
  assets: GitHubReleaseAsset[];
}

export interface UpdateInfo {
  available: boolean;
  latestVersion: string;
  releaseNotes: string;
  apkUrl: string | null;
  apkFileName: string | null;
  error?: string;
}

/**
 * Сравнивает версии по числам (major.minor.patch), убирая префикс "v".
 */
export function compareVersionsNumeric(remoteTag: string, currentVersion: string = APP_VERSION): number {
  const cleanRemote = remoteTag.trim().replace(/^[vV]/, "").split("-")[0];
  const cleanCurrent = currentVersion.trim().replace(/^[vV]/, "").split("-")[0];

  const rParts = cleanRemote.split(".").map((n) => parseInt(n, 10) || 0);
  const cParts = cleanCurrent.split(".").map((n) => parseInt(n, 10) || 0);

  const len = Math.max(rParts.length, cParts.length);
  for (let i = 0; i < len; i++) {
    const r = rParts[i] ?? 0;
    const c = cParts[i] ?? 0;
    if (r > c) return 1;
    if (r < c) return -1;
  }
  return 0;
}

/**
 * Запрашивает последний релиз через GitHub API:
 * https://api.github.com/repos/vovik06302-ai/avtoServis/releases/latest
 */
export async function checkForGithubUpdate(): Promise<UpdateInfo> {
  try {
    const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases/latest`, {
      headers: {
        Accept: "application/vnd.github+json",
      },
    });

    if (!response.ok) {
      return {
        available: false,
        latestVersion: APP_VERSION,
        releaseNotes: "",
        apkUrl: null,
        apkFileName: null,
        error: `Не удалось получить релиз с GitHub (код ${response.status})`,
      };
    }

    const data: GitHubReleaseResponse = await response.json();
    const latestVersion = (data.tag_name || "").replace(/^[vV]/, "");
    const isNewer = compareVersionsNumeric(data.tag_name, APP_VERSION) > 0;
    const apkAsset = (data.assets || []).find((a) => a.name.toLowerCase().endsWith(".apk"));

    return {
      available: isNewer,
      latestVersion,
      releaseNotes: data.body || "Обновление приложения",
      apkUrl: apkAsset ? apkAsset.browser_download_url : null,
      apkFileName: apkAsset ? apkAsset.name : null,
    };
  } catch (err) {
    return {
      available: false,
      latestVersion: APP_VERSION,
      releaseNotes: "",
      apkUrl: null,
      apkFileName: null,
      error: "Нет интернета или GitHub недоступен. Приложение продолжает работать офлайн.",
    };
  }
}
