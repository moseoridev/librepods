.class public Ldev/moseori/ksuprobe/ProcessObserver;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

.field private final process:Ljava/lang/Process;

.method public constructor <init>(Ljava/lang/Process;)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Ldev/moseori/ksuprobe/ProcessObserver;->process:Ljava/lang/Process;
    return-void
.end method

.method public run()V
    .locals 5
    const-string v0, "KSUProbe"
    :try_start
    iget-object v1, p0, Ldev/moseori/ksuprobe/ProcessObserver;->process:Ljava/lang/Process;
    invoke-virtual {v1}, Ljava/lang/Process;->getErrorStream()Ljava/io/InputStream;
    move-result-object v2
    new-instance v3, Ljava/io/InputStreamReader;
    invoke-direct {v3, v2}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V
    new-instance v2, Ljava/io/BufferedReader;
    invoke-direct {v2, v3}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :read
    invoke-virtual {v2}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;
    move-result-object v3
    if-eqz v3, :finished
    invoke-static {v0, v3}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
    goto :read
    :finished
    invoke-virtual {v2}, Ljava/io/BufferedReader;->close()V
    invoke-virtual {v1}, Ljava/lang/Process;->waitFor()I
    move-result v1
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    const-string v3, "su exit code: "
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v3
    invoke-static {v0, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch
    return-void
    :catch
    move-exception v4
    const-string v3, "Cannot observe su"
    invoke-static {v0, v3, v4}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    return-void
.end method
