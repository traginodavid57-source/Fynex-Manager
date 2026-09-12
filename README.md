# ⚡ Fynex — Gerenciador de Arquivos & Power Suite Open-Source

<p align="center">
  <b>A fusão definitiva entre o design moderno do Fylo e o poder absoluto de engenharia reversa do MT Manager.</b><br>
  <i>100% Gratuito • Sem Assinaturas • Sem Recursos Bloqueados • Código Aberto</i>
</p>

---

## 🌟 O que é o Fynex?

O **Fynex** nasceu da necessidade da comunidade de ter um gerenciador de arquivos que combine:
1. **A interface limpa, elegante e moderna do Fylo** (construída em Jetpack Compose, Material 3 Expressive, animações fluidas, suporte a temas AMOLED e IA nativa).
2. **As ferramentas avançadas para power users do MT Manager** (painel duplo interativo, manipulação direta de arquivos compactados ZIP/APK sem extração total, inspeção e assinatura de APKs, decodificador de AndroidManifest.xml binário, editor de código e hexadecimal, comparador Diff de textos e comandos root).
3. **Liberdade total (FOSS)**: Zero assinaturas mensais ou anuais (ao contrário do Fylo Pro e do MT VIP). Todos os recursos estão desbloqueados para todos.

---

## 📊 Comparativo: Fynex vs. MT Manager vs. Fylo

| Recurso / Funcionalidade | MT Manager | Fylo | **Fynex (Este Projeto)** |
|---|:---:|:---:|:---:|
| **Licença** | Proprietário / Fechado | Proprietário / Freesub | **100% Open Source (GPL v3)** |
| **Preço / Assinatura** | VIP Mensal / Anual | Pro Plan Pago | **Gratuito / Doação Voluntária** |
| **Interface** | Clássica Android Views | Material 3 (Compose) | **Material 3 Expressive (Compose)** |
| **Navegador Painel Duplo (Dual Pane)** | ✅ (Clássico) | ⚠️ (Limitado) | ✅ **Completo com Sync & Swap** |
| **Navegação & Edição Direta em ZIP/APK** | ✅ | ❌ | ✅ **Direto na Memória/Zip4j** |
| **Decodificador AndroidManifest.xml (AXML)** | ✅ | ⚠️ | ✅ **Nativo em Kotlin** |
| **Assinador de APK (v1/v2 TestKey/Custom)** | ✅ (VIP para alguns) | ❌ | ✅ **Totalmente Gratuito** |
| **Inspetor DEX (Classes, Métodos, Assinatura)** | ✅ | ❌ | ✅ **DEX Inspector Integrado** |
| **Editor de Código com Linhas e Regex** | ✅ | ⚠️ | ✅ **Syntax Highlight + Busca Regex** |
| **Editor e Visualizador Hexadecimal** | ✅ | ⚠️ | ✅ **Offset + Hex + ASCII** |
| **Comparador de Arquivos (Diff)** | ✅ (VIP) | ❌ | ✅ **Diff Viewer Lado a Lado** |
| **Acesso Root (Superusuário / su / libsu)** | ✅ | ⚠️ | ✅ **Suporte Completo a Root** |
| **Assistente de IA & Resumo de Documentos** | ❌ | ✅ (Pago) | ✅ **Gemini Grátis, Claude, Ollama** |
| **Sistema de Plugins (.fynexplug)** | ⚠️ (Limitado) | ✅ | ✅ **Módulos, Model Packs & Ferramentas** |
| **Servidor PC Web Transfer (Wi-Fi sem cabos)** | ❌ | ✅ | ✅ **Servidor HTTP Embutido** |
| **Cofre Criptografado (AES-256-GCM)** | ❌ | ✅ (Pro) | ✅ **Cofre Seguro com Biometria** |

---

## 🚀 Principais Módulos do Fynex

### 📂 1. Explorador & Painel Duplo (MT Manager Style)
- **Painéis Lado a Lado ou por Abas Rápidas**: Alterne fluidamente entre o Painel 1 (Esquerda) e Painel 2 (Direita).
- **Ações Rápidas de Transferência**:
  - `Copiar para o outro lado ➔`
  - `Mover para o outro lado ➔`
  - `Sincronizar Caminhos`
  - `Inverter Lados (Swap)`
- **Navegação Transparente em Arquivos Compactados**: Abra arquivos `.zip`, `.apk`, `.jar`, `.tar` diretamente como se fossem pastas comuns, sem a necessidade de descompactar o arquivo inteiro.
- **Barra de Navegação Breadcrumb Dinâmica**: Navegue pelos diretórios pais com apenas um clique.

### ⚡ 2. Suíte de Engenharia Reversa de APK (MT Mode)
- **APK Inspector**:
  - Exibe nome do app, pacote, versão, versionCode, minSdk, targetSdk.
  - Verificação de assinaturas digitais (v1 Jar e v2/v3 APK Signature Scheme).
  - Listagem detalhada de permissões (com realce em vermelho para permissões perigosas como câmera, localização e armazenamento).
  - Detecção de arquiteturas nativas (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
  - Contagem de arquivos DEX, Activities, Services e Receivers.
- **Decodificador Binário XML**: Decodifica arquivos binários compilados `AndroidManifest.xml` e layouts XML para XML legível e editável.
- **Assinador de APK (ApkSignerUtil)**: Assina APKs modificados usando chaves AOSP TestKey ou Keystores personalizadas com algoritmo SHA-256 with RSA e geração dos manifestos `META-INF/`.
- **Inspetor DEX (DexInspector)**: Lê cabeçalhos de arquivos `.dex`, informando total de classes, métodos, campos, strings e soma de verificação.

### 📝 3. Editor de Código & Hexadecimal
- **CodeEditorScreen**:
  - Numeração de linhas, quebra de linha opcional (word-wrap).
  - Localizar e substituir com suporte a expressões regulares (Regex).
  - Botão integrado **"🤖 Explicar com IA"** para tirar dúvidas sobre trechos de código em tempo real.
- **HexEditorScreen**:
  - Visualização em 3 colunas: Offset Hexadecimal, Bytes Hex (com destaque para bytes nulos e valores imprimíveis) e Decodificação ASCII.

### 🤖 4. Inteligência Artificial Aberta (Fylo AI Architecture)
- Suporte a múltiplos provedores sem nenhum custo de assinatura:
  - **Google Gemini** (Chave de API gratuita do Google AI Studio recomendada).
  - **Ollama Local** (Execução 100% offline direto no Termux ou rede local: `http://127.0.0.1:11434`).
  - **OpenAI / Groq / Claude** (Compatível com qualquer endpoint padrão OpenAI).
- Funcionalidades:
  - Resumo de documentos longos (PDF, Markdown, TXT, DOCX).
  - Explicação e auditoria de segurança de código fonte e Manifests de APK.
  - Chat contextual com arquivos anexados.

### 📦 5. Sistema de Extensões & Plugins
- Gerenciador de plugins inspirado no Fylo e no MT Manager.
- Estrutura de pacotes abertos (`plugin.json` / `.fynexplug`):
  - **Módulos de Ferramentas**: Conversores, scripts customizados.
  - **Visualizadores**: Suporte a novos formatos de arquivo.
  - **Model Packs de IA**: Modelos offline GGUF / Whisper.

### 🌐 6. PC Web Transfer (Servidor Local Wi-Fi)
- Servidor HTTP leve e portátil embutido.
- Permite acessar a memória interna pelo navegador de qualquer computador ou celular na mesma rede local digitando a URL indicada (ex: `http://192.168.1.15:8080`).
- Download de arquivos e navegação sem fios.

### 🔐 7. Cofre Criptografado
- Criptografia autenticada militar **AES-256-GCM**.
- Protegido por senha mestra ou biometria.
- Isolamento total de arquivos confidenciais.

---

## 💖 Como Apoiar o Projeto (Doações Voluntárias)

O Fynex não possui patrocinadores corporativos nem cobra assinaturas. Se o app é útil para você e você deseja apoiar a continuidade do desenvolvimento de software livre:

### 🇧🇷 PIX (Brasil)
- **Chave Pix**: `fynex.opensource@gmail.com`

### 🌐 Internacional / Crowdfunding
- [GitHub Sponsors](https://github.com/sponsors)
- [Buy Me a Coffee](https://buymeacoffee.com)
- [Ko-fi](https://ko-fi.com)

### 🪙 Criptomoedas
- **Bitcoin (BTC)**: `bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh`
- **Monero (XMR)**: `888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkFxbANsAnJYPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H`
- **Ethereum / USDT (ERC-20 / BEP-20)**: `0x71C83602167d4F88D0c554e2098675c9A95C1709`

---

## 🛠️ Como Compilar o Projeto

Requisitos:
- JDK 17
- Android SDK (API 35/36)
- Gradle 8.x

No terminal (Linux / macOS / Termux):
```bash
./gradlew assembleDebug
```
O arquivo APK compilado estará disponível em `app/build/outputs/apk/debug/app-debug.apk`.
