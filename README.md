# OfflineNotes

![OfflineNotes Banner](https://i.imgur.com/Wvebbw5.png)

<div align="center">

[![License: AGPL v3](https://img.shields.io/badge/License-AGPLv3-blue.svg?style=for-the-badge)](LICENSE)
[![Stars](https://img.shields.io/github/stars/bashln/bashnotes?style=for-the-badge)](https://github.com/bashln/bashnotes/stargazers)
[![Forks](https://img.shields.io/github/forks/bashln/bashnotes?style=for-the-badge)](https://github.com/bashln/bashnotes/network/members)
[![Issues](https://img.shields.io/github/issues/bashln/bashnotes?style=for-the-badge)](https://github.com/bashln/bashnotes/issues)
[![Last Commit](https://img.shields.io/github/last-commit/bashln/bashnotes?style=for-the-badge)](https://github.com/bashln/bashnotes/commits/main)

</div>

OfflineNotes is an Android notes app built to be simple, private, and offline-first.
Source of truth is plain text files (`.org` and `.md`) in a user-selected folder.

## Instalacao rapida / Quick install

- GitHub Releases: https://github.com/bashln/bashnotes/releases
- Baixe / Download: `OfflineNotes-v<versionName>+<versionCode>-release.apk`
- Android: permita instalacao de fontes desconhecidas / allow unknown sources when prompted

## PT-BR

### Visao geral
- 100% offline para o uso principal (anotacoes).
- Sem backend, sem conta, sem telemetria e sem analytics.
- Usa SAF (`ACTION_OPEN_DOCUMENT_TREE`) para leitura/escrita de arquivos.
- Arquivos `.org` e `.md` sao a fonte de verdade.

### Recursos
- Lista e edicao de notas locais.
- Criacao rapida de nota (`.org` padrao; alternavel para markdown).
- Checklist por texto (`- [ ]`, `- [x]`, `- [X]`).
- Renomear, excluir, auto-save.
- Modo `Edit` e `View` no editor.
- Preview basico para Org/Markdown (headers, listas, checklists, negrito/italico, inline code, links).

### Como rodar
```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Instalacao
- Download do APK pela pagina de releases do GitHub:
  https://github.com/bashln/bashnotes/releases
- Baixe o arquivo mais recente `OfflineNotes-v<versionName>+<versionCode>-release.apk`.
- Instale no Android e permita instalacao de fontes desconhecidas quando solicitado.

### Distribuicao via GitHub Releases (APK release)
```bash
./gradlew :app:assembleRelease
ls app/build/outputs/apk/release/

git tag -a v0.1.0 -m "OfflineNotes v0.1.0"
git push origin v0.1.0

gh release create v0.1.0 \
  app/build/outputs/apk/release/OfflineNotes-v0.1.0+1-release.apk \
  --title "OfflineNotes v0.1.0" \
  --notes "## Highlights
- ...
"
```

> As credenciais de assinatura release sao lidas por variaveis de ambiente.
> Veja `RELEASING.md`.

### Comandos de validacao
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lint
./gradlew :app:assembleDebug
```

### Privacidade (resumo)
- O app nao precisa de internet para funcionar.
- O app nao envia seus dados para servidores.
- O app nao coleta dados pessoais, nao rastreia uso e nao usa SDK de analytics.
- Todo conteudo fica sob controle do usuario, na pasta selecionada via SAF.
- Seus dados sao seus, apenas seus.

Politica completa: veja `PRIVACY.md`.

## EN

### Overview
- Fully offline for the core use case (note taking).
- No backend, no account, no telemetry, no analytics.
- Uses SAF (`ACTION_OPEN_DOCUMENT_TREE`) for file read/write access.
- `.org` and `.md` files are the source of truth.

### Features
- Local note listing and editing.
- Quick note creation (`.org` by default, switchable to markdown).
- Text checklist support (`- [ ]`, `- [x]`, `- [X]`).
- Rename, delete, auto-save.
- `Edit` and `View` editor modes.
- Basic Org/Markdown preview (headings, lists, checklists, bold/italic, inline code, links).

### Run locally
```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Installation
- Download the APK from GitHub Releases:
  https://github.com/bashln/bashnotes/releases
- Download the latest `OfflineNotes-v<versionName>+<versionCode>-release.apk` asset.
- Install it on Android and allow installation from unknown sources when prompted.

### GitHub Releases distribution (release APK)
```bash
./gradlew :app:assembleRelease
ls app/build/outputs/apk/release/

git tag -a v0.1.0 -m "OfflineNotes v0.1.0"
git push origin v0.1.0

gh release create v0.1.0 \
  app/build/outputs/apk/release/OfflineNotes-v0.1.0+1-release.apk \
  --title "OfflineNotes v0.1.0" \
  --notes "## Highlights
- ...
"
```

> Release signing credentials are loaded from environment variables.
> See `RELEASING.md`.

### Validation commands
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lint
./gradlew :app:assembleDebug
```

### Privacy (summary)
- The app does not require internet access to work.
- The app does not upload your notes to remote servers.
- The app does not collect personal data, track usage, or include analytics SDKs.
- All data stays under user control in the SAF-selected folder.
- Your data is yours, and only yours.

Full policy: see `PRIVACY.md`.

## License

This project is licensed under **GNU AGPLv3**.

If you distribute a modified version, you must provide the corresponding source code under AGPLv3.
AGPL obligations can also apply when modified versions are provided for network use.

See `LICENSE`.

## Third-party credits

OfflineNotes uses Android and Jetpack libraries (AndroidX / Google ecosystem), such as Compose,
Navigation, DataStore, DocumentFile, Material Components, and JUnit for tests.

These dependencies are from third-party open-source projects and should be acknowledged.
See `THIRD_PARTY_NOTICES.md` for a concise notice list.
