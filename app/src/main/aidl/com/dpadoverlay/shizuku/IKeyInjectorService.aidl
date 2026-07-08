package com.dpadoverlay.shizuku;

interface IKeyInjectorService {
    void destroy();
    void exit();
    boolean injectKey(int keyCode);
}
