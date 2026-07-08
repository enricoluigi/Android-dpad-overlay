# D-pad Overlay

App Android com overlay flutuante que simula um teclado direcional com teclas para **cima**, **baixo**, **esquerda**, **direita**, **voltar** e **home**.

## Download

**[Baixar APK v1.0.0](https://github.com/enricoluigi/Android-dpad-overlay/releases/download/v1.0.0/dpad-overlay-v1.0.0.apk)**

Página de releases: https://github.com/enricoluigi/Android-dpad-overlay/releases

## Funcionalidades

- Overlay flutuante sobre qualquer aplicativo
- Layout em cruz (D-pad) com botões direcionais
- Botões **Voltar** e **Home** integrados
- Arraste o overlay pela alça superior para reposicionar
- Serviço em primeiro plano para manter o overlay ativo

## Requisitos

- Android 8.0 (API 26) ou superior
- Permissão **Exibir sobre outros apps**
- Serviço de **Acessibilidade** ativado para o app

## Como usar

1. Instale o APK no dispositivo ou emulador
2. Abra o app e conceda a permissão de overlay
3. Ative o serviço de acessibilidade **D-pad Overlay** nas configurações do sistema
4. Toque em **Iniciar overlay**
5. Use os botões do overlay para navegar em outros apps

## Build

```bash
./gradlew assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Notas técnicas

- **Voltar** e **Home** usam ações globais do serviço de acessibilidade e funcionam na maioria dos dispositivos
- As teclas direcionais (`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`) são injetadas via `InputManager` ou comando shell `input keyevent`
- Em dispositivos com restrições de segurança mais rígidas, as teclas direcionais podem exigir emulador, Android TV ou permissões elevadas (root/Shizuku)
- Ideal para Android TV, set-top boxes e emuladores onde apps dependem de navegação por D-pad

## Estrutura

```
app/src/main/java/com/dpadoverlay/
├── MainActivity.kt              # Tela principal e permissões
├── DpadOverlayApp.kt            # Application e canal de notificação
└── service/
    ├── OverlayService.kt        # Overlay flutuante
    ├── DpadAccessibilityService.kt  # Injeção de teclas
    └── KeyInjector.kt           # Estratégias de injeção
```
