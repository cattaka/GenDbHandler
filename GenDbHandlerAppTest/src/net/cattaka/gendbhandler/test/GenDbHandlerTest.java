
package net.cattaka.gendbhandler.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.cattaka.gendbhandler.test.db.TestOpenHelper;
import net.cattaka.gendbhandler.test.model.UserModel;
import net.cattaka.gendbhandler.test.model.UserModel.Authority;
import net.cattaka.gendbhandler.test.model.UserModel.Role;
import net.cattaka.gendbhandler.test.model.handler.UserModelHandler;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

public class GenDbHandlerTest extends AndroidTestCase {
    public static final String TEST_DB_NAME = "test.db";

    private TestOpenHelper mHelper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File dbFile = getContext().getDatabasePath(TEST_DB_NAME);
        if (dbFile.exists()) {
            if (!dbFile.delete()) {
                throw new RuntimeException("Deleting test.db. failed.");
            }
        }
        mHelper = new TestOpenHelper(getContext(), TEST_DB_NAME);
    }

    public void testDml() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        { // Insert
            UserModel model;
            { // create model
                List<String> tags = new ArrayList<String>();
                tags.add("Java");
                tags.add("PHP");
                model = new UserModel(null, "taro", "Taro Yamada", "A", Role.PROGRAMMER,
                        new Date(), tags, Authority.ADMIN);
            }
            UserModelHandler.insert(db, model);
            UserModel model2 = UserModelHandler.findByUsername(db, "taro");
            assertEquals(model.getId(), model2.getId());
            assertEquals(model.getUsername(), model2.getUsername());
            assertEquals(model.getTeam(), model2.getTeam());
            assertEquals(model.getRole(), model2.getRole());
            assertEquals(model.getCreatedAt(), model2.getCreatedAt());
            assertEquals(model.getTags().size(), model2.getTags().size());
            assertEquals("Java", model2.getTags().get(0));
            assertEquals("PHP", model2.getTags().get(1));
            assertEquals(Authority.ADMIN, model2.getAuthority());
        }
        { // Update
            UserModel model = UserModelHandler.findByUsername(db, "taro");
            { // update model
                List<String> tags = new ArrayList<String>();
                tags.add("Java");
                tags.add("PHP");
                tags.add("Ruby");
                model.setUsername("taro2");
                model.setNickname("Taro Yamada2");
                model.setTeam("B");
                model.setRole(Role.MANAGER);
                model.setCreatedAt(new Date());
                model.setTags(tags);
            }
            long n = UserModelHandler.update(db, model);
            assertEquals(1L, n);
            UserModel model2 = UserModelHandler.findByUsername(db, "taro2");
            assertEquals(model.getId(), model2.getId());
            assertEquals(model.getUsername(), model2.getUsername());
            assertEquals(model.getTeam(), model2.getTeam());
            assertEquals(model.getRole(), model2.getRole());
            assertEquals(model.getCreatedAt(), model2.getCreatedAt());
            assertEquals(model.getTags().size(), model2.getTags().size());
            assertEquals("Java", model2.getTags().get(0));
            assertEquals("PHP", model2.getTags().get(1));
        }
        { // delete
            UserModel model = UserModelHandler.findByUsername(db, "taro2");
            UserModelHandler.delete(db, model.getId());
            UserModel model2 = UserModelHandler.findByUsername(db, "taro2");
            assertNull(model2);
        }

        db.close();
    }

    public void testFind() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        UserModelHandler.insert(db, new UserModel(null, "taro", "Taro Yamada", "A",
                Role.PROGRAMMER, new Date(), null, Authority.ADMIN));
        UserModelHandler.insert(db, new UserModel(null, "hana", "Hana Yamada", "A", Role.DESIGNNER,
                new Date(), null, Authority.ADMIN));
        UserModelHandler.insert(db, new UserModel(null, "yuji", "Yuji Tanaka", "B",
                Role.PROGRAMMER, new Date(), null, Authority.ADMIN));
        UserModelHandler.insert(db, new UserModel(null, "chun", "Chun Tanaka", "B", Role.DESIGNNER,
                new Date(), null, Authority.USER));
        { // findById
            UserModel model = UserModelHandler.findById(db, 2L);
            assertEquals("hana", model.getUsername());
        }
        { // findByUsername
            UserModel model = UserModelHandler.findByUsername(db, "yuji");
            assertEquals("yuji", model.getUsername());
        }
        { // findByTeamOrderByRoleAscAndIdAsc
            List<UserModel> models = UserModelHandler.findByTeamOrderByRoleAscAndIdAsc(db, 0, "A");
            assertEquals(2, models.size());
            assertEquals("hana", models.get(0).getUsername());
            assertEquals("taro", models.get(1).getUsername());
        }
        { // findByTeamOrderByRoleAscAndIdDesc
            List<UserModel> models = UserModelHandler.findByTeamOrderByIdDesc(db, 0, "B");
            assertEquals(2, models.size());
            assertEquals("chun", models.get(0).getUsername());
            assertEquals("yuji", models.get(1).getUsername());
        }
        { // findByTeamOrderByRoleAscAndIdDesc
            List<UserModel> models = UserModelHandler.findByAuthorityOrderByIdAsc(db, 0,
                    Authority.ADMIN);
            assertEquals(3, models.size());
            assertEquals("taro", models.get(0).getUsername());
            assertEquals("hana", models.get(1).getUsername());
            assertEquals("yuji", models.get(2).getUsername());
        }
    }
}
