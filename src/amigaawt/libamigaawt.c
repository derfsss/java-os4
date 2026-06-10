/*
 * libamigaawt -- minimal AmigaOS 4 windowing JNI for the Java-OS4 AWT bring-up.
 *
 * Phase 4 M3: open an Intuition window on the Workbench screen, blit a Java
 * ARGB framebuffer into it via graphics.library WritePixelArray, and poll
 * IDCMP events.  Swing paints everything through Java2D, so this surface +
 * events is the complete native windowing contract.
 *
 * Phase 4 M4: second JNI export set for sun.awt.amiga.AmigaNative (the real
 * AWT toolkit), keymap.library RAWKEY->char mapping, screen size query and
 * window resize.  The legacy AmigaWindow exports remain for the M3 WinTest.
 *
 * GPLv2+Classpath-exception (java-os4 project).
 */
#include <exec/types.h>
#include <exec/libraries.h>
#include <intuition/intuition.h>
#include <graphics/rastport.h>
#include <graphics/blitattr.h>
#include <devices/inputevent.h>
#include <proto/exec.h>
#include <proto/intuition.h>
#include <proto/graphics.h>
#include <proto/keymap.h>

#include <string.h>

#include "jni.h"

extern struct ExecIFace *IExec;

/* definitions for the globals the proto headers declare extern */
struct IntuitionBase *IntuitionBase = NULL;
struct GfxBase *GfxBase = NULL;
struct Library *KeymapBase = NULL;
struct IntuitionIFace *IIntuition = NULL;
struct GraphicsIFace *IGraphics = NULL;
struct KeymapIFace *IKeymap = NULL;

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
    /* keymap is optional (chars degrade to 0 without it) */
    KeymapBase = IExec->OpenLibrary("keymap.library", 51);
    if (KeymapBase != NULL)
        IKeymap = (struct KeymapIFace *)
            IExec->GetInterface(KeymapBase, "main", 1, NULL);
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

static jlong do_open(JNIEnv *env, jint w, jint h, jstring title)
{
    struct Window *win;
    const char *t;
    char *tcopy;

    if (!ensure_libs())
        return 0;

    /* Intuition keeps the title POINTER for the window's lifetime -- it must
       outlive the JNI UTF buffer (copy freed in do_close) */
    t = (*env)->GetStringUTFChars(env, title, NULL);
    tcopy = IExec->AllocVecTags(strlen(t) + 1, TAG_DONE);
    if (tcopy != NULL)
        strcpy(tcopy, t);
    win = IIntuition->OpenWindowTags(NULL,
        WA_Title,         (ULONG)tcopy,
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
    if (win == NULL && tcopy != NULL)
        IExec->FreeVec(tcopy);

    return (jlong)(uintptr_t)win;
}

static void do_blit(JNIEnv *env, jlong handle, jintArray pixels,
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

/* out[0]=rawcode out[1]=mouseX out[2]=mouseY out[3]=qualifier
   out[4]=mapped char (RAWKEY downs, 0 if none)  out[5..7]=reserved.
   Writes `n` slots; returns the event type. */
static jint do_poll(JNIEnv *env, jlong handle, jintArray out, int n)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    struct IntuiMessage *msg;
    jint vals[8] = { 0, 0, 0, 0, 0, 0, 0, 0 };
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
            if (msg->Code & IECODE_UP_PREFIX) {
                type = EV_KEY_UP;
            } else {
                type = EV_KEY_DOWN;
                if (IKeymap != NULL) {
                    struct InputEvent ie;
                    char buf[8];
                    LONG nc;
                    ie.ie_NextEvent = NULL;
                    ie.ie_Class = IECLASS_RAWKEY;
                    ie.ie_SubClass = 0;
                    ie.ie_Code = msg->Code;
                    ie.ie_Qualifier = msg->Qualifier;
                    ie.ie_EventAddress = NULL;
                    nc = IKeymap->MapRawKey(&ie, buf, sizeof(buf), NULL);
                    if (nc > 0)
                        vals[4] = (jint)(unsigned char)buf[0];
                }
            }
            break;
        case IDCMP_NEWSIZE:
            vals[1] = win->Width - win->BorderLeft - win->BorderRight;
            vals[2] = win->Height - win->BorderTop - win->BorderBottom;
            type = EV_NEWSIZE;
            break;
        case IDCMP_REFRESHWINDOW:
            IIntuition->BeginRefresh(win);
            IIntuition->EndRefresh(win, TRUE);
            type = EV_REFRESH;
            break;
        default: break;
    }
    IExec->ReplyMsg((struct Message *)msg);

    (*env)->SetIntArrayRegion(env, out, 0, n, vals);
    return type;
}

static void do_close(jlong handle)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    STRPTR ttl;
    if (win == NULL)
        return;
    ttl = win->Title;   /* our AllocVec copy (open or settitle) */
    IIntuition->CloseWindow(win);
    if (ttl != NULL)
        IExec->FreeVec(ttl);
}

/* ------------- legacy M3 exports (class AmigaWindow, out[4]) ------------- */

JNIEXPORT jlong JNICALL
Java_AmigaWindow_open0(JNIEnv *env, jclass cls, jint w, jint h, jstring title)
{
    return do_open(env, w, h, title);
}

JNIEXPORT void JNICALL
Java_AmigaWindow_blit0(JNIEnv *env, jclass cls, jlong handle, jintArray pixels,
                       jint x, jint y, jint w, jint h, jint stride)
{
    do_blit(env, handle, pixels, x, y, w, h, stride);
}

JNIEXPORT jint JNICALL
Java_AmigaWindow_poll0(JNIEnv *env, jclass cls, jlong handle, jintArray out)
{
    return do_poll(env, handle, out, 4);
}

JNIEXPORT void JNICALL
Java_AmigaWindow_close0(JNIEnv *env, jclass cls, jlong handle)
{
    do_close(handle);
}

/* --------- M4 exports (class sun.awt.amiga.AmigaNative, out[8]) ---------- */

JNIEXPORT jlong JNICALL
Java_sun_awt_amiga_AmigaNative_open0(JNIEnv *env, jclass cls,
                                     jint w, jint h, jstring title)
{
    return do_open(env, w, h, title);
}

JNIEXPORT void JNICALL
Java_sun_awt_amiga_AmigaNative_blit0(JNIEnv *env, jclass cls, jlong handle,
                                     jintArray pixels, jint x, jint y,
                                     jint w, jint h, jint stride)
{
    do_blit(env, handle, pixels, x, y, w, h, stride);
}

JNIEXPORT jint JNICALL
Java_sun_awt_amiga_AmigaNative_poll0(JNIEnv *env, jclass cls, jlong handle,
                                     jintArray out)
{
    return do_poll(env, handle, out, 8);
}

JNIEXPORT void JNICALL
Java_sun_awt_amiga_AmigaNative_close0(JNIEnv *env, jclass cls, jlong handle)
{
    do_close(handle);
}

/* screen size of the (public) Workbench screen: (w << 16) | h, 0 on failure */
JNIEXPORT jint JNICALL
Java_sun_awt_amiga_AmigaNative_screensize0(JNIEnv *env, jclass cls)
{
    struct Screen *scr;
    jint r = 0;

    if (!ensure_libs())
        return 0;
    scr = IIntuition->LockPubScreen(NULL);
    if (scr != NULL) {
        r = ((jint)scr->Width << 16) | (jint)(scr->Height & 0xFFFF);
        IIntuition->UnlockPubScreen(NULL, scr);
    }
    return r;
}

/* resize the window so the inner (GZZ) area becomes w x h */
JNIEXPORT void JNICALL
Java_sun_awt_amiga_AmigaNative_resize0(JNIEnv *env, jclass cls, jlong handle,
                                       jint w, jint h)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    if (win == NULL)
        return;
    IIntuition->ChangeWindowBox(win, win->LeftEdge, win->TopEdge,
        w + win->BorderLeft + win->BorderRight,
        h + win->BorderTop + win->BorderBottom);
}

/* move the window so the OUTER top-left is at x,y on the screen */
JNIEXPORT void JNICALL
Java_sun_awt_amiga_AmigaNative_move0(JNIEnv *env, jclass cls, jlong handle,
                                     jint x, jint y)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    if (win == NULL)
        return;
    IIntuition->ChangeWindowBox(win, x, y, win->Width, win->Height);
}

/* set the window title (Intuition keeps the pointer: install an AllocVec
   copy, free the previous one) */
JNIEXPORT void JNICALL
Java_sun_awt_amiga_AmigaNative_settitle0(JNIEnv *env, jclass cls, jlong handle,
                                         jstring title)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    const char *t;
    char *copy;
    STRPTR old;

    if (win == NULL)
        return;
    t = (*env)->GetStringUTFChars(env, title, NULL);
    copy = IExec->AllocVecTags(strlen(t) + 1, TAG_DONE);
    if (copy != NULL) {
        strcpy(copy, t);
        old = win->Title;
        IIntuition->SetWindowTitles(win, copy, (CONST_STRPTR)~0);
        if (old != NULL)
            IExec->FreeVec(old);
    }
    (*env)->ReleaseStringUTFChars(env, title, t);
}

/* bring to front / send to back */
JNIEXPORT void JNICALL
Java_sun_awt_amiga_AmigaNative_tofront0(JNIEnv *env, jclass cls, jlong handle)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    if (win != NULL)
        IIntuition->WindowToFront(win);
}

JNIEXPORT void JNICALL
Java_sun_awt_amiga_AmigaNative_toback0(JNIEnv *env, jclass cls, jlong handle)
{
    struct Window *win = (struct Window *)(uintptr_t)handle;
    if (win != NULL)
        IIntuition->WindowToBack(win);
}
