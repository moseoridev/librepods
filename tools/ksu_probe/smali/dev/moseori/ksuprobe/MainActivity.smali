.class public Ldev/moseori/ksuprobe/MainActivity;
.super Landroid/app/Activity;
.source "MainActivity.java"

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 8

    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    new-instance v0, Landroid/widget/TextView;
    invoke-direct {v0, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    const/4 v1, -0x1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V
    const v1, -0x1000000
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setBackgroundColor(I)V
    const/high16 v1, 0x41c00000    # 24.0f
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextSize(F)V
    const/16 v1, 0x30
    invoke-virtual {v0, v1, v1, v1, v1}, Landroid/widget/TextView;->setPadding(IIII)V
    const-string v1, "KernelSU root probe\n\nRequesting su..."
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    invoke-virtual {p0, v0}, Landroid/app/Activity;->setContentView(Landroid/view/View;)V

    :try_start_0
    invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
    move-result-object v1
    const-string v2, "cmd_b64"
    invoke-virtual {v1, v2}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2

    if-nez v2, :command_ready
    const-string v2, "aWQ7IGdldGVuZm9yY2U7IGNhdCAvcHJvYy8xL2F0dHIvY3VycmVudA=="

    :command_ready
    new-instance v3, Ljava/lang/StringBuilder;
    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V
    const-string v4, "exec > /sdcard/Download/ksu_probe.txt 2>&1; printf '%s' '"
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v4, "' | base64 -d | sh; rc=$?; echo __KSU_PROBE_RC__=$rc"
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v3

    const/4 v4, 0x3
    new-array v4, v4, [Ljava/lang/String;
    const/4 v5, 0x0
    const-string v6, "su"
    aput-object v6, v4, v5
    const/4 v5, 0x1
    const-string v6, "-c"
    aput-object v6, v4, v5
    const/4 v5, 0x2
    aput-object v3, v4, v5

    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;
    move-result-object v7
    invoke-virtual {v7, v4}, Ljava/lang/Runtime;->exec([Ljava/lang/String;)Ljava/lang/Process;

    move-result-object v5
    new-instance v3, Ldev/moseori/ksuprobe/ProcessObserver;
    invoke-direct {v3, v5}, Ldev/moseori/ksuprobe/ProcessObserver;-><init>(Ljava/lang/Process;)V
    new-instance v4, Ljava/lang/Thread;
    invoke-direct {v4, v3}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
    invoke-virtual {v4}, Ljava/lang/Thread;->start()V

    const-string v1, "KernelSU root probe\n\nRequest sent. Grant this app in KernelSU if prompted."
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v1
    const-string v2, "KSUProbe"
    const-string v3, "Cannot start su"
    invoke-static {v2, v3, v1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    invoke-virtual {v1}, Ljava/lang/Exception;->toString()Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v0, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    return-void
.end method
