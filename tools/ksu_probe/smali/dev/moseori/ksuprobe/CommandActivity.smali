.class public Ldev/moseori/ksuprobe/CommandActivity;
.super Landroid/app/Activity;
.source "CommandActivity.java"

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 7

    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    :try_start_0
    invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
    move-result-object v0
    const-string v1, "cmd_b64"
    invoke-virtual {v0, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0

    if-nez v0, :command_ready
    const-string v0, "aWQ7IGdldGVuZm9yY2U7IGNhdCAvcHJvYy8xL2F0dHIvY3VycmVudA=="

    :command_ready
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "exec > /sdcard/Download/ksu_probe.txt 2>&1; printf '%s' '"
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v2, "' | base64 -d | sh; rc=$?; echo __KSU_PROBE_RC__=$rc"
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1

    const/4 v2, 0x3
    new-array v2, v2, [Ljava/lang/String;
    const/4 v3, 0x0
    const-string v4, "su"
    aput-object v4, v2, v3
    const/4 v3, 0x1
    const-string v4, "-c"
    aput-object v4, v2, v3
    const/4 v3, 0x2
    aput-object v1, v2, v3

    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;
    move-result-object v5
    invoke-virtual {v5, v2}, Ljava/lang/Runtime;->exec([Ljava/lang/String;)Ljava/lang/Process;
    move-result-object v5
    new-instance v0, Ldev/moseori/ksuprobe/ProcessObserver;
    invoke-direct {v0, v5}, Ldev/moseori/ksuprobe/ProcessObserver;-><init>(Ljava/lang/Process;)V
    new-instance v1, Ljava/lang/Thread;
    invoke-direct {v1, v0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
    invoke-virtual {v1}, Ljava/lang/Thread;->start()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    :done
    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    return-void

    :catch_0
    move-exception v6
    const-string v0, "KSUProbe"
    const-string v1, "Cannot start su"
    invoke-static {v0, v1, v6}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    goto :done
.end method
