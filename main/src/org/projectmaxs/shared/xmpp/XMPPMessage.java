/*
    This file is part of Project MAXS.

    MAXS and its modules is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MAXS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MAXS.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.projectmaxs.shared.xmpp;

import android.os.Parcel;
import android.os.Parcelable;

public class XMPPMessage implements Parcelable {

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public static final Creator<XMPPMessage> CREATOR = new Creator<XMPPMessage>() {

		@Override
		public XMPPMessage createFromParcel(Parcel source) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public XMPPMessage[] newArray(int size) {
			// TODO Auto-generated method stub
			return null;
		}

	};

}