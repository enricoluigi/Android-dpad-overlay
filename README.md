# D-pad Overlay

App Android com overlay flutuante que envia **teclas reais de controle remoto** (`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, `DPAD_CENTER`, `BACK`, `HOME`) para qualquer aplicativo.

## Download

**[Baixar APK v1.1.0](https://github.com/enricoluigi/Android-dpad-overlay/releases/download/v1.1.0/dpad-overlay-v1.1.0.apk)**

Página de releases: https://github.com/enricoluigi/Android-dpad-overlay/releases

## Como funciona

O Android **não permite** que apps comuns injetem teclas direcionais sem permissões elevadas. Este app usa o **[Shizuku](https://shizuku.rikka.app/)** para executar `input keyevent` com privilégios de shell/ADB — o mesmo mecanismo usado por apps de mapeamento de teclas.

Isso envia teclas **de verdade**, como um joystick ou controle remoto físico.

## Configuração (uma vez)

### 1. Instalar o Shizuku
- Baixe em https://shizuku.rikka.app/download/
- Ou instale pela Play Store / F-Droid

### 2. Iniciar o Shizuku (sem root)
1. Ative **Opções do desenvolvedor** no Android
2. Ative **Depuração sem fio** (Android 11+)
3. Abra o app **Shizuku** → **Iniciar via Depuração sem fio**
4. Siga as instruções na tela (pareamento com um toque)

> Após reiniciar o celular, é preciso iniciar o Shizuku de novo.

### 3. Configurar o D-pad Overlay
1. Instale o APK do D-pad Overlay
2. Conceda **Exibir sobre outros apps**
3. Toque em **Conceder permissão ao D-pad Overlay** (Shizuku)
4. Inicie o overlay

### 4. Acessibilidade (opcional)
Só necessária como fallback para **Voltar/Home** se o Shizuku não estiver ativo.

## Layout do controle

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

## Notas

- **Shizuku é obrigatório** para as teclas direcionais funcionarem como controle remoto
- Funciona em jogos, emuladores, Android TV e apps que usam D-pad
- Não requer root
- Requer reiniciar o Shizuku após reboot do dispositivo
