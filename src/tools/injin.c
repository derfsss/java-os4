/*
 * injin -- inject input events through input.device (AmigaOS 4).
 *
 * Drives REAL input into the active screen/window for automated GUI testing
 * (the qemu QMP socket is held by the fleet MCP, so host-side input
 * injection is unavailable; this runs guest-side instead).
 *
 * Usage (commands are processed left to right):
 *   injin POS <x> <y>          move the pointer to absolute screen x,y
 *   injin CLICK                left button down+up at the current position
 *   injin RIGHTCLICK           right button down+up
 *   injin KEY <code> [S]       rawkey press+release (hex ok: 0x25); S=shift
 *   injin WAIT <ms>            delay between events
 * Commands can be chained:  injin POS 260 150 CLICK WAIT 200 KEY 0x25
 *
 * GPLv2+Classpath-exception (java-os4 project).
 */
#include <exec/types.h>
#include <exec/io.h>
#include <exec/ports.h>
#include <devices/input.h>
#include <devices/inputevent.h>
#include <proto/exec.h>
#include <proto/dos.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static struct MsgPort *port;
static struct IOStdReq *ioreq;

static int send_event(struct InputEvent *ie)
{
    ioreq->io_Command = IND_WRITEEVENT;
    ioreq->io_Data = ie;
    ioreq->io_Length = sizeof(struct InputEvent);
    return IExec->DoIO((struct IORequest *)ioreq) == 0;
}

static void ev_clear(struct InputEvent *ie)
{
    memset(ie, 0, sizeof(*ie));
}

static int do_pos(int x, int y)
{
    struct InputEvent ie;
    ev_clear(&ie);
    ie.ie_Class = IECLASS_POINTERPOS;
    ie.ie_position.ie_xy.ie_x = x;
    ie.ie_position.ie_xy.ie_y = y;
    return send_event(&ie);
}

static int do_button(UWORD downcode, UWORD downqual)
{
    struct InputEvent ie;
    int ok;

    ev_clear(&ie);
    ie.ie_Class = IECLASS_RAWMOUSE;
    ie.ie_Code = downcode;
    ie.ie_Qualifier = downqual;
    ok = send_event(&ie);
    usleep(80000);

    ev_clear(&ie);
    ie.ie_Class = IECLASS_RAWMOUSE;
    ie.ie_Code = downcode | IECODE_UP_PREFIX;
    ok &= send_event(&ie);
    return ok;
}

static int do_key(UWORD code, UWORD qual)
{
    struct InputEvent ie;
    int ok;

    ev_clear(&ie);
    ie.ie_Class = IECLASS_RAWKEY;
    ie.ie_Code = code;
    ie.ie_Qualifier = qual;
    ok = send_event(&ie);
    usleep(60000);

    ev_clear(&ie);
    ie.ie_Class = IECLASS_RAWKEY;
    ie.ie_Code = code | IECODE_UP_PREFIX;
    ie.ie_Qualifier = qual;
    ok &= send_event(&ie);
    return ok;
}

int main(int argc, char **argv)
{
    int i = 1, ok = 1;

    port = (struct MsgPort *)IExec->AllocSysObjectTags(ASOT_PORT, TAG_DONE);
    ioreq = (struct IOStdReq *)IExec->AllocSysObjectTags(ASOT_IOREQUEST,
        ASOIOR_Size, sizeof(struct IOStdReq),
        ASOIOR_ReplyPort, port,
        TAG_DONE);
    if (port == NULL || ioreq == NULL
            || IExec->OpenDevice("input.device", 0,
                                 (struct IORequest *)ioreq, 0) != 0) {
        printf("injin: cannot open input.device\n");
        return 10;
    }

    while (i < argc && ok) {
        if (!strcmp(argv[i], "POS") && i + 2 < argc) {
            ok = do_pos(atoi(argv[i + 1]), atoi(argv[i + 2]));
            i += 3;
        } else if (!strcmp(argv[i], "CLICK")) {
            ok = do_button(IECODE_LBUTTON, IEQUALIFIER_LEFTBUTTON);
            i++;
        } else if (!strcmp(argv[i], "RIGHTCLICK")) {
            ok = do_button(IECODE_RBUTTON, IEQUALIFIER_RBUTTON);
            i++;
        } else if (!strcmp(argv[i], "KEY") && i + 1 < argc) {
            UWORD code = (UWORD)strtol(argv[i + 1], NULL, 0);
            UWORD qual = 0;
            i += 2;
            if (i < argc && !strcmp(argv[i], "S")) {
                qual = IEQUALIFIER_LSHIFT;
                i++;
            }
            ok = do_key(code, qual);
        } else if (!strcmp(argv[i], "WAIT") && i + 1 < argc) {
            usleep(atoi(argv[i + 1]) * 1000);
            i += 2;
        } else {
            printf("injin: bad arg '%s'\n", argv[i]);
            ok = 0;
        }
        usleep(50000);
    }

    IExec->CloseDevice((struct IORequest *)ioreq);
    IExec->FreeSysObject(ASOT_IOREQUEST, ioreq);
    IExec->FreeSysObject(ASOT_PORT, port);
    printf("injin: %s\n", ok ? "OK" : "FAILED");
    return ok ? 0 : 10;
}
