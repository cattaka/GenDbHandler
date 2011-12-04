package net.cattaka.gendbhandler.test;

import java.io.File;

import net.cattaka.gendbhandler.test.db.TestOpenHelper;

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
	
	public void tesbtDml() {
		
	}
	
//	public void testUpdateDatabase() {
//		{
//			WalttendDbHelperV1 dbh = new WalttendDbHelperV1(getContext(), TEST_DB_NAME);
//			dbh.insertRecordCountDto(new RecordCountDto(201101220120L, 201101220125L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//			dbh.insertRecordCountDto(new RecordCountDto(201101220125L, 201101220130L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//			dbh.getWritableDatabase().close();
//			List<RecordCountDto> dtos = dbh.findByStartTime(201101220120L, 201102220120L);
//			dbh.close();
//			assertEquals(2, dtos.size());
//		}
//		{
//			WalttendDbHelper dbh = new WalttendDbHelper(getContext(), TEST_DB_NAME);
//			SQLiteDatabase db = dbh.getWritableDatabase();
//			db.rawQuery("SELECT * FROM VALUE_HOLDER", new String[0]).close();
//			db.rawQuery("SELECT * FROM SYNC_STATE", new String[0]).close();
//			db.rawQuery("SELECT * FROM RECORD_COUNT", new String[0]).close();
//			dbh.close();
//		}
//	}
//	
//	public void testValueHolder() {
//		WalttendDbHelper dbh = new WalttendDbHelper(getContext(), TEST_DB_NAME);
//		SQLiteDatabase db = dbh.getWritableDatabase();
//		ValueHolderDto srcDto = new ValueHolderDto();
//		srcDto.setKey("THE_KEY");
//		dbh.registValueHolderDto(db, srcDto);
//		
//		{
//			ValueHolderDto dstDto = dbh.findValueHolderDto(db, "THE_KEY");
//			assertEquals(srcDto.getKey(), dstDto.getKey());
//			assertEquals(srcDto.getNumValue(), dstDto.getNumValue());
//			assertEquals(srcDto.getTextValue(), dstDto.getTextValue());
//		}
//		
//		srcDto.setNumValue(3L);
//		srcDto.setTextValue("The text");
//		dbh.registValueHolderDto(db, srcDto);
//		
//		{
//			ValueHolderDto dstDto = dbh.findValueHolderDto(db, "THE_KEY");
//			assertEquals(srcDto.getKey(), dstDto.getKey());
//			assertEquals(srcDto.getNumValue(), dstDto.getNumValue());
//			assertEquals(srcDto.getTextValue(), dstDto.getTextValue());
//		}
//	}
//	
//	public void testSyncState() {
//		WalttendDbHelper dbh = new WalttendDbHelper(getContext(), TEST_DB_NAME);
//		SQLiteDatabase db = dbh.getWritableDatabase();
//		
//		SyncStateDto srcDto = new SyncStateDto(0, 1000, new Date(2000), new Date(3000));
//		dbh.registSyncStateDto(db, srcDto);
//		
//		{
//			List<SyncStateDto> dstDtos = dbh.findSyncStateAll(db);
//			assertEquals(1, dstDtos.size());
//			assertEquals(srcDto.getYearMonth(), dstDtos.get(0).getYearMonth());
//			assertEquals(srcDto.getRemoteUpdateAt(), dstDtos.get(0).getRemoteUpdateAt());
//			assertEquals(srcDto.getLastSyncAt(), dstDtos.get(0).getLastSyncAt());
//		}
//		
//		SyncStateDto srcDto2 = new SyncStateDto(0, 1000, new Date(2500), new Date(3500));
//		dbh.registSyncStateDto(db, srcDto2);
//		{
//			List<SyncStateDto> dstDtos = dbh.findSyncStateAll(db);
//			assertEquals(1, dstDtos.size());
//			assertEquals(srcDto2.getYearMonth(), dstDtos.get(0).getYearMonth());
//			assertEquals(srcDto2.getRemoteUpdateAt(), dstDtos.get(0).getRemoteUpdateAt());
//			assertEquals(srcDto2.getLastSyncAt(), dstDtos.get(0).getLastSyncAt());
//		}
//		
//		SyncStateDto srcDto3 = new SyncStateDto(0, 1001, new Date(2500), new Date(3500));
//		dbh.registSyncStateDto(db, srcDto3);
//		{
//			List<SyncStateDto> dstDtos = dbh.findSyncStateAll(db);
//			assertEquals(2, dstDtos.size());
//		}
//	}
//	
//	public void testProduceNewStateId() {
//		WalttendDbHelper dbh = new WalttendDbHelper(getContext(), TEST_DB_NAME);
//		dbh.insertRecordCountDto(new RecordCountDto(201103011010L, 201103011015L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201103011015L, 201103011020L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201104011015L, 201104011020L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.produceNewStateId();
//		
//		{
//			SQLiteDatabase db = dbh.getReadableDatabase();
//			List<SyncStateDto> syncStateDtos = dbh.findSyncStateAll(db);
//			db.close();
//			assertEquals(2, syncStateDtos.size());
//			assertEquals(2011*12 + 3 - 1, syncStateDtos.get(0).getYearMonth());
//			assertEquals(2011*12 + 4 - 1, syncStateDtos.get(1).getYearMonth());
//		}
//
//		dbh.insertRecordCountDto(new RecordCountDto(201104011020L, 201104011025L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.produceNewStateId();
//		{
//			SQLiteDatabase db = dbh.getReadableDatabase();
//			List<SyncStateDto> syncStateDtos = dbh.findSyncStateAll(db);
//			db.close();
//			assertEquals(2, syncStateDtos.size());
//		}
//		
//		dbh.insertRecordCountDto(new RecordCountDto(201105011020L, 201105011025L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.produceNewStateId();
//		{
//			SQLiteDatabase db = dbh.getReadableDatabase();
//			List<SyncStateDto> syncStateDtos = dbh.findSyncStateAll(db);
//			db.close();
//			assertEquals(3, syncStateDtos.size());
//		}
//	}
//	
//	public void testCreateNextUploadRecordDto() {
//		WalttendDbHelper dbh = new WalttendDbHelper(getContext(), TEST_DB_NAME);
//		SQLiteDatabase db = dbh.getWritableDatabase();
//		assertNull(dbh.createNextUploadRecordDto(db));
//		
//		dbh.insertRecordCountDto(db, new RecordCountDto(201102010000L, 201102010010L, 10, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(db, new RecordCountDto(201102010010L, 201102010020L, 11, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(db, new RecordCountDto(201102010000L, 201102010010L, 5, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(db, new RecordCountDto(201102010000L, 201102010010L, 99, RecordCountDto.RECORD_FLAG_INNER_DISCARD));
//		dbh.insertRecordCountDto(db, new RecordCountDto(201102010010L, 201102010020L, 99, RecordCountDto.RECORD_FLAG_OUTER_DISCARED));
//		dbh.insertRecordCountDto(db, new RecordCountDto(201102010010L, 201102010020L, 99, RecordCountDto.RECORD_FLAG_OUTER_VALID));
//		UploadRecordDto urDto1 = dbh.createNextUploadRecordDto(db);
//		assertNotNull(urDto1);
//		assertEquals(2011, urDto1.getYear());
//		assertEquals(1, urDto1.getMonth());
//		assertEquals(15, urDto1.getRecords()[0]);
//		assertEquals(11, urDto1.getRecords()[1]);
//		assertEquals(Integer.MAX_VALUE, urDto1.getRecords()[2]);
//	}
//
//	public void testDiscardByYearMonth() {
//		WalttendDbHelper dbh = new WalttendDbHelper(getContext(), TEST_DB_NAME);
//		dbh.insertRecordCountDto(new RecordCountDto(201101010000L, 201101010010L, 1, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201101010000L, 201101010010L, 2, RecordCountDto.RECORD_FLAG_OUTER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201102010000L, 201102010010L, 3, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201102010000L, 201102010010L, 4, RecordCountDto.RECORD_FLAG_OUTER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201103010000L, 201103010010L, 5, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201103010000L, 201103010010L, 6, RecordCountDto.RECORD_FLAG_OUTER_VALID));
//		
//		{
//			List<RecordCountDto> dtos = dbh.findByStartTime(201101010000L, 201104010000L);
//			assertEquals(6, dtos.size());
//		}
//		
//		SQLiteDatabase db = dbh.getWritableDatabase();
//		db.beginTransaction();
//		dbh.discardByYearMonth(db, 2011, 2-1);
//		db.setTransactionSuccessful();
//		db.endTransaction();
//		db.close();
//		
//		{
//			List<RecordCountDto> dtos = dbh.findByStartTime(201101000000L, 201104000000L);
//			assertEquals(4, dtos.size());
//			assertEquals(201101010000L, dtos.get(0).getStartTime());
//			assertEquals(201101010000L, dtos.get(1).getStartTime());
//			assertEquals(201103010000L, dtos.get(2).getStartTime());
//			assertEquals(201103010000L, dtos.get(3).getStartTime());
//			
//			assertEquals(1, dtos.get(0).getCount());
//			assertEquals(2, dtos.get(1).getCount());
//			assertEquals(5, dtos.get(2).getCount());
//			assertEquals(6, dtos.get(3).getCount());
//
//			assertEquals(RecordCountDto.RECORD_FLAG_INNER_VALID, dtos.get(0).getRecordFlag());
//			assertEquals(RecordCountDto.RECORD_FLAG_OUTER_VALID, dtos.get(1).getRecordFlag());
//			assertEquals(RecordCountDto.RECORD_FLAG_INNER_VALID, dtos.get(2).getRecordFlag());
//			assertEquals(RecordCountDto.RECORD_FLAG_OUTER_VALID, dtos.get(3).getRecordFlag());
//		}
//	}
//	public void testResetRecordCount() {
//		WalttendDbHelper dbh = new WalttendDbHelper(getContext(), TEST_DB_NAME);
//		dbh.insertRecordCountDto(new RecordCountDto(201101010000L, 201101010010L, 1, RecordCountDto.RECORD_FLAG_INNER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201101020000L, 201101020010L, 2, RecordCountDto.RECORD_FLAG_OUTER_VALID));
//		dbh.insertRecordCountDto(new RecordCountDto(201102030000L, 201102030010L, 3, RecordCountDto.RECORD_FLAG_INNER_DISCARD));
//		dbh.insertRecordCountDto(new RecordCountDto(201102040000L, 201102040010L, 4, RecordCountDto.RECORD_FLAG_OUTER_DISCARED));
//		
//		dbh.registSyncStateDto(new SyncStateDto(0, 2011*12 + 3, new Date(), new Date()));
//		dbh.registSyncStateDto(new SyncStateDto(0, 2011*12 + 4, new Date(), new Date()));
//		dbh.registSyncStateDto(new SyncStateDto(0, 2011*12 + 5, new Date(), new Date()));
//		
//		dbh.registValueHolderDto(new ValueHolderDto(0, WalttendDbHelper.KEY_LAST_STATE_ID, 123, null));
//		dbh.registValueHolderDto(new ValueHolderDto(0, WalttendDbHelper.KEY_LAST_UPLOAD_ID, 234, null));
//		
//		dbh.resetRecordCount();
//		
//		List<RecordCountDto> rcs = dbh.findByStartTime(201101010000L, 201201010000L);
//		assertEquals(2, rcs.size());
//		assertEquals(201101010000L, rcs.get(0).getStartTime());
//		assertEquals(1, rcs.get(0).getCount());
//		assertEquals(RecordCountDto.RECORD_FLAG_INNER_VALID, rcs.get(0).getRecordFlag());
//		assertEquals(201102030000L, rcs.get(1).getStartTime());
//		assertEquals(3, rcs.get(1).getCount());
//		assertEquals(RecordCountDto.RECORD_FLAG_INNER_VALID, rcs.get(1).getRecordFlag());
//		
//		List<SyncStateDto> ssds = dbh.findSyncStateAll();
//		assertEquals(0, ssds.size());
//		
//		assertEquals(0, dbh.findValueHolderDto(WalttendDbHelper.KEY_LAST_STATE_ID).getNumValue());
//		assertEquals(0, dbh.findValueHolderDto(WalttendDbHelper.KEY_LAST_UPLOAD_ID).getNumValue());
//	}
}
