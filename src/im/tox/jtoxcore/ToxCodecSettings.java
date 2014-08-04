/* ToxFileControl.java
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

package im.tox.jtoxcore;

public class ToxCodecSettings {
    public ToxCallType call_type;

    public int video_bitrate;/* In kbits/s */
    public int max_video_width; /* In px */
    public int max_video_height; /* In px */

    public int audio_bitrate; /* In bits/s */
    public int audio_frame_duration; /* In ms */
    public int audio_sample_rate; /* In Hz */
    public int audio_channels;

    public ToxCodecSettings() {
        super();
    }
    public ToxCodecSettings(ToxCallType c, int vb, int mvw, int mvh, int ab, int afd, int asr, int ac) {
        super();
        this.call_type = c;
        this.video_bitrate = vb;
        this.max_video_width = mvw;
        this.max_video_height = mvh;
        this.audio_bitrate = ab;
        this.audio_frame_duration = afd;
        this.audio_sample_rate = asr;
        this.audio_channels = ac;
    }
}
