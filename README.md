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
- Formato padrao de nova nota: `.org` (alteravel no menu da tela `Notas`).

## Como selecionar pasta
- Abra o app e toque no menu (hamburger) na tela `Notas`.
- Toque em `Selecionar pasta`.
- Escolha uma pasta usando o seletor do Android (SAF).
- Escolha uma pasta com permissao de escrita (ex.: `Documents/OfflineNotes`).
- O app salva a permissao persistente para reabrir sem pedir novamente.

## Formato padrao de novas notas
- No menu da tela `Notas`, use `Formato padrao: Org/Markdown` para alternar.
- Toque normal no FAB cria usando esse formato padrao.
- Pressione e segure o FAB para escolher pontualmente entre `.md`, checklist `.md` e `.org`.

## Se der erro de permissao
- Mensagem esperada: `Sem permissao para acessar a pasta. Selecione a pasta novamente.`
- Toque em `Selecionar pasta` no aviso e escolha a pasta de novo.
- Se aparecer erro de escrita, selecione outra pasta onde o app possa criar arquivos.
- O app usa somente URIs `content://` do SAF para ler e salvar arquivos.
