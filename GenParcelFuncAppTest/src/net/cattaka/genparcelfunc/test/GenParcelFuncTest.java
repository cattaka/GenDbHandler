
package net.cattaka.genparcelfunc.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.cattaka.genparcelfunc.test.model.UserModel;
import net.cattaka.genparcelfunc.test.model.UserModel.Authority;
import net.cattaka.genparcelfunc.test.model.UserModel.Role;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

public class GenParcelFuncTest extends AndroidTestCase {
    public void testNull() {
        { // Insert
            UserModel model;
            { // create model
                List<String> tags = new ArrayList<String>();
                model = new UserModel();
            }
            byte[] data;
            {
                Parcel parcel = Parcel.obtain();
                model.writeToParcel(parcel, 0);
                data = parcel.marshall();
                parcel.recycle();
            }

            UserModel model2;
            {
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(data, 0, data.length);
                parcel.setDataPosition(0);
                model2 = UserModel.CREATOR.createFromParcel(parcel);
            }
            assertNull(model2.getId());
            assertNull(model2.getUsername());
            assertNull(model2.getTeam());
            assertNull(model2.getRole());
            assertNull(model2.getCreatedAt());
            assertNull(model2.getTags());
            assertNull(model2.getAuthority());
            assertNull(model2.getBlob());
            assertNull(model2.getBlob());
        }
    }

    public void testNotNull() {
        { // Insert
            UserModel model;
            { // create model
                List<String> tags = new ArrayList<String>();
                tags.add("Java");
                tags.add("PHP");
                model = new UserModel(null, "taro", "Taro Yamada", "A", Role.PROGRAMMER,
                        new Date(), tags, Authority.ADMIN, new byte[] {
                                1, 2
                        });
            }
            byte[] data;
            {
                Parcel parcel = Parcel.obtain();
                model.writeToParcel(parcel, 0);
                data = parcel.marshall();
                parcel.recycle();
            }

            UserModel model2;
            {
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(data, 0, data.length);
                parcel.setDataPosition(0);
                model2 = UserModel.CREATOR.createFromParcel(parcel);
            }
            assertEquals(model.getId(), model2.getId());
            assertEquals(model.getUsername(), model2.getUsername());
            assertEquals(model.getTeam(), model2.getTeam());
            assertEquals(model.getRole(), model2.getRole());
            assertEquals(model.getCreatedAt(), model2.getCreatedAt());
            assertEquals(model.getTags().size(), model2.getTags().size());
            assertEquals("Java", model2.getTags().get(0));
            assertEquals("PHP", model2.getTags().get(1));
            assertEquals(Authority.ADMIN, model2.getAuthority());
            assertNotNull(model2.getBlob());
            MoreAsserts.assertEquals(model.getBlob(), model2.getBlob());
        }
    }
}
