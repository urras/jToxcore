/* callbacks.h
 *
 *  Copyright (C) 2014 Tox project All Rights Reserved.
 *
 *  This file is part of jToxcore
 *
 *  jToxcore is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  jToxcore is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with jToxcore.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
static void callback_friendrequest(Tox *, uint8_t *, uint8_t *, uint16_t, void *);
static void callback_friendmessage(Tox *, int, uint8_t *, uint16_t, void *);
static void callback_action(Tox *, int32_t, uint8_t *, uint16_t, void *);
static void callback_namechange(Tox *, int32_t, uint8_t *, uint16_t, void *);
static void callback_statusmessage(Tox *, int32_t, uint8_t *, uint16_t, void *);
static void callback_userstatus(Tox *, int32_t, uint8_t, void *);
static void callback_read_receipt(Tox *, int32_t, uint32_t, void *);
static void callback_connectionstatus(Tox *, int32_t, uint8_t, void *);
static void callback_typingstatus(Tox *, int32_t, uint8_t, void *);
static void callback_filecontrol(Tox *, int32_t, uint8_t, uint8_t, uint8_t, uint8_t *, uint16_t, void *);
static void callback_filedata(Tox *, int32_t, uint8_t, uint8_t *, uint16_t, void *);
static void callback_filesendrequest(Tox *, int32_t, uint8_t, uint64_t, uint8_t *, uint16_t, void *);
static void avcallback_invite(void *, int32_t, void *);
static void avcallback_start(void *, int32_t, void *);
static void avcallback_cancel(void *, int32_t, void *);
static void avcallback_reject(void *, int32_t, void *);
static void avcallback_end(void *, int32_t, void *);
static void avcallback_ringing(void *, int32_t, void *);
static void avcallback_starting(void *, int32_t, void *);
static void avcallback_ending(void *, int32_t, void *);
static void avcallback_requesttimeout(void *, int32_t, void *);
static void avcallback_peertimeout(void *, int32_t, void *);
static void avcallback_mediachange(void *, int32_t, void *);
static void avcallback_audio(ToxAv *, int32_t, int16_t *, int, void *);
static void avcallback_video(ToxAv *, int32_t,  vpx_image_t *, void *);


