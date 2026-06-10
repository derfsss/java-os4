/*
 * libamigaawt -- minimal AmigaOS 4 windowing JNI for the Java-OS4 AWT bring-up.
 *
 * Phase 4 M3: open an Intuition window on the Workbench screen, blit a Java
 * ARGB framebuffer into it via graphics.library WritePixelArray, and poll
 * IDCMP events.  Swing paints everything through Java2D, so this surface +
 * events is the complete native windowing contract.
 *
 * GPLv2+Classpath-exception (java-os4 project).
 */
#include <exec/types.h>
#include <exec/libraries.h>
#include <intuition/intuition.h>
#include <graphics/rastport.h>
#include <graphics/blitattr.h>
#include <proto/exec.h>
#include <proto/intuition.h>
#include <proto/graphics.h>

#include "jni.h"

extern struct ExecIFace *IExec;

/* definitions for the globals the proto headers declare extern */
struct IntuitionBase *IntuitionBase = NULL;
struct GfxBase *GfxBase = NULL;
struct IntuitionIFace *IIntuition = NULL;
struct GraphicsIFace *IGraphics = NULL;

static int ensure_libs(void) {
    if (IIntuition != NULL && IGraphics != NULL)
        return 1;
    IntuitionBase = (struct IntuitionBase *)
        IExec->OpenLibrary("intuition.library", 51);
    GfxBase = (struct GfxBase *)IExec->OpenLibrary("graphics.library", 51);
    if (IntuitionBase == NULL || GfxBase == NULL)
        return 0;
    IIntuition = (struct IntuitionIFace *)
        IExec->GetInterface((struct Library *)IntuitionBase, "main", 1, NULL);
    IGraphics = (struct GraphicsIFace *)
        IExec->GetInterface((struct Library *)GfxBase, "main", 1, NULL);
    return IIntuition != NULL && IGraphics != NULL;
}

/* event type codes shared with the Java side */
#define EV_NONE        0
#define EV_CLOSE       1
#define EV_MOUSE_DOWN  2
#define EV_MOUSE_UP    3
#define EV_MOUSE_MOVE  4
#define EV_KEY_DOWN    5
#define EV_KEY_UP      6
#define EV_NEWSIZE     7
#define EV_REFRESH     8

JNIEXPORT jlong JNICALL
Java_AmigaWindow_open0(JNIEnv *env, jclass cls, jint w, jint h, jstring title)
{
    struct Window *win;
    const char *t;

    if (!ensure_libs())
        return 0;

    t = (*env)->GetStringUTFChars(env, title, NULL);
    win = IIntuition->OpenWindowTags(NULL,
        WA_Title,         (ULONG)t,
        WA_InnerWidth,    w,
        WA_InnerHeight,   h,
        WA_Left,          80,
        WA_Top,           60,
        WA_GimmeZeroZero, TRUE,
        WA_CloseGadget,   TRUE,
        WA_DragBar,       TRUE,
        WA_DepthGadget,   TRUE,
        WA_Activate,      TRUE,
        WA_SimpleRefresh, TRUE,
        WA_IDCMP,         IDCMP_CLOSEWINDOW | IDCMP_MOUSEBUTTONS |
                          IDCMP_MOUSEMOVE | IDCMP_RAWKEY |
                          IDCMP_NEWSIZE | IDCMP_REFRESHWINDOW,
        WA_ReportMouse,   TRUE,
        TAG_DONE);
    (*env)->ReleaseStringUTFChars(env, title, t);

    return (jlong)(uintptr_t)win;
}

JNIEXPORT void JNICALL
Java_AmigaWindow_blit0(JNIEnv *env, jclass cls, jlong handle, jintArray pixels,
                       jint x, jint y, jint w, jint h, jint stride)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    jint *px;

    if (win == NULL)
        return;

    px = (*env)->GetPrimitiveArrayCritical(env, pixels, NULL);
    if (px == NULL)
        return;

    /* ARGB32 rows -> the window's (GimmeZeroZero) rastport */
    IGraphics->WritePixelArray((uint8 *)px, x, y, stride * 4,
                               PIXF_A8R8G8B8, win->RPort,
                               x, y, w, h);

    (*env)->ReleasePrimitiveArrayCritical(env, pixels, px, JNI_ABORT);
}

/* out[0]=code out[1]=mouseX out[2]=mouseY out[3]=qualifier; returns ev type */
JNIEXPORT jint JNICALL
Java_AmigaWindow_poll0(JNIEnv *env, jclass cls, jlong handle, jintArray out)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    struct IntuiMessage *msg;
    jint vals[4] = { 0, 0, 0, 0 };
    jint type = EV_NONE;

    if (win == NULL)
        return EV_NONE;

    msg = (struct IntuiMessage *)IExec->GetMsg(win->UserPort);
    if (msg == NULL)
        return EV_NONE;

    vals[0] = msg->Code;
    vals[1] = msg->MouseX;
    vals[2] = msg->MouseY;
    vals[3] = msg->Qualifier;

    switch (msg->Class) {
        case IDCMP_CLOSEWINDOW:  type = EV_CLOSE; break;
        case IDCMP_MOUSEBUTTONS:
            type = (msg->Code & IECODE_UP_PREFIX) ? EV_MOUSE_UP : EV_MOUSE_DOWN;
            break;
        case IDCMP_MOUSEMOVE:    type = EV_MOUSE_MOVE; break;
        case IDCMP_RAWKEY:
            type = (msg->Code & IECODE_UP_PREFIX) ? EV_KEY_UP : EV_KEY_DOWN;
            break;
        case IDCMP_NEWSIZE:      type = EV_NEWSIZE; break;
        case IDCMP_REFRESHWINDOW:
            IIntuition->BeginRefresh(win);
            IIntuition->EndRefresh(win, TRUE);
            type = EV_REFRESH;
            break;
        default: break;
    }
    IExec->ReplyMsg((struct Message *)msg);

    (*env)->SetIntArrayRegion(env, out, 0, 4, vals);
    return type;
}

JNIEXPORT void JNICALL
Java_AmigaWindow_close0(JNIEnv *env, jclass cls, jlong handle)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    if (win != NULL)
        IIntuition->CloseWindow(win);
}
