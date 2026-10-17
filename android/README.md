# Umami Analytics — Android Native App

Aplicativo Android nativo compilado para a plataforma **Umami Analytics Cloud** (`https://umami-chi-topaz.vercel.app`).

## 🚀 Arquitetura & Otimizações Nativas

- **Hardware Acceleration (WebGL 2.0)**: Aceleração por GPU ativada nativamente (`LAYER_TYPE_HARDWARE`), garantindo renderização a 120 FPS dos shaders WebGL de cristal líquido e estética MCP OS.
- **AMOLED Immersive Black (`#000000`)**: Layout *edge-to-edge* com barras de status e navegação transparentes, eliminando flashes brancos e barras de URL de navegador.
- **Sessão Permanente (10 Anos)**: Integração com `CookieManager` e `WebStorage` nativos, preservando tokens de autenticação permanentemente entre reinicializações do app.
- **Navegação Inteligente**:
  - `OnBackPressedCallback` nativo para retornar no histórico de sites e filtros do Umami sem fechar a aplicação acidentalmente.
  - Links externos abrem automaticamente no navegador padrão do sistema.
- **Pull-to-Refresh**: `SwipeRefreshLayout` nativo com cores do tema Umami (`#8b5cf6` e `#18181b`).
- **Retrocompatibilidade**: Compatível de Android 7.0 (API 24) a Android 15+ (API 35+).

## 🛠️ Compilação

Para compilar o APK assinado para produção:

```bash
cd android
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew assembleRelease --no-daemon
```

O APK gerado e assinado estará em:
`app/build/outputs/apk/release/app-release.apk`

## 📲 Instalação via ADB

```bash
# Conectar via ADB sem fio ou USB
adb connect <IP_DO_DISPOSITIVO>:<PORTA>

# Instalar APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Iniciar aplicação
adb shell am start -n com.blackpiratex.umami/.MainActivity
```