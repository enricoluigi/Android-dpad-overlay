# D-pad Overlay

App Android com overlay flutuante que envia teclas de **controle remoto** para qualquer aplicativo.

## Download

**[Baixar APK v1.2.1](https://github.com/enricoluigi/Android-dpad-overlay/releases/download/v1.2.1/dpad-overlay-v1.2.1.apk)**

## Configuração rápida (2 passos)

1. Conceda **Exibir sobre outros apps**
2. Ative o **serviço de acessibilidade** do D-pad Overlay
3. Toque em **Iniciar overlay**

Sem apps extras, sem root, sem Shizuku.

## Como funciona

Usa a API oficial do Android (`performGlobalAction`) — a mesma abordagem de apps como **Key Mapper**:

| Tecla | API |
|-------|-----|
| ▲ ▼ ◀ ▶ | `GLOBAL_ACTION_DPAD_*` |
| OK | `GLOBAL_ACTION_DPAD_CENTER` |
| Voltar | `GLOBAL_ACTION_BACK` |
| Home | `GLOBAL_ACTION_HOME` |

## Requisitos

- Android 8.0+ (API 26) para overlay
- **Android 13+ (API 33)** para teclas direcionais e OK
- Voltar/Home funcionam em qualquer versão suportada

## Layout

```
      [▲]
[◀]  [OK]  [▶]
      [▼]
[Voltar] [Home]
```

## Build

```bash
./gradlew assembleDebug
```
