/* Minimal clib4 pthread smoke test. Isolates whether clib4 pthread_create works
   at all when launched outside a Shell/CLI process (vs. a JamVM-specific cause). */
#include <pthread.h>
#include <stdio.h>
#include <errno.h>
#include <proto/exec.h>
#include <proto/dos.h>

static void *worker(void *arg) {
    (void)arg;
    DebugPrintF("[PT] worker thread ran\n");
    return NULL;
}

int main(void) {
    pthread_t tid;
    int rc;

    DebugPrintF("[PT] Input=%p Output=%p ErrorOutput=%p\n",
                (void*)Input(), (void*)Output(), (void*)ErrorOutput());

    if(ErrorOutput() == 0) {
        BPTR nil = Open("NIL:", MODE_NEWFILE);
        SelectErrorOutput(nil);
        DebugPrintF("[PT] opened NIL:=%p, ErrorOutput now=%p\n",
                    (void*)nil, (void*)ErrorOutput());
    }

    rc = pthread_create(&tid, NULL, worker, NULL);
    DebugPrintF("[PT] pthread_create rc=%d errno=%d\n", rc, errno);

    if(rc == 0) {
        pthread_join(tid, NULL);
        DebugPrintF("[PT] joined OK\n");
    }

    printf("pthreadtest done rc=%d\n", rc);
    return rc;
}
