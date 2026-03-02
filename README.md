# OfflineNotes

Offline-first Android notes app focused on speed and simplicity.

## Stack
- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- DataStore Preferences
- SAF (`ACTION_OPEN_DOCUMENT_TREE`) for file-based notes (`.md` and `.org`)

## Notes
- No built-in sync, no network layer.
- Files are the source of truth.
- Bottom navigation contains only `Notas` and `Sync`.

## Como selecionar pasta
- Abra o app e toque no icone de pasta na tela `Notas`.
- Escolha uma pasta usando o seletor do Android (SAF).
- O app salva a permissao persistente para reabrir sem pedir novamente.

## Se der erro de permissao
- Mensagem esperada: `Sem permissao para acessar a pasta. Selecione a pasta novamente.`
- Toque em `Selecionar pasta` no aviso e escolha a pasta de novo.
- O app usa somente URIs `content://` do SAF para ler e salvar arquivos.
