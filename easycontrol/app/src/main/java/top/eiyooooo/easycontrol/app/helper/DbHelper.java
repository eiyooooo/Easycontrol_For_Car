package top.eiyooooo.easycontrol.app.helper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import top.eiyooooo.easycontrol.app.entity.AppData;
import top.eiyooooo.easycontrol.app.entity.Device;

public class DbHelper extends SQLiteOpenHelper {

  private static final String dataBaseName = "app.db";
  private static final int version = 18;
  private final String tableName = "DevicesDb";

  public DbHelper(Context context) {
    super(context, dataBaseName, null, version);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + tableName + " (\n" + "\t uuid text PRIMARY KEY,\n" + "\t type integer,\n" + "\t name text,\n" + "\t address text,\n" + "\t specified_app text,\n" + "\t isAudio integer,\n" + "\t maxSize integer,\n" + "\t maxFps integer,\n" + "\t maxVideoBit integer,\n" + "\t setResolution integer,\n" + "\t defaultFull integer,\n" + "\t useH265 integer,\n" + "\t useOpus integer,\n" + "\t connectOnStart integer,\n" + "\t clipboardSync integer,\n" + "\t nightModeSync integer\n" + ");");
  }

  @SuppressLint("Range")
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < version) {
      // 获取旧数据
      ArrayList<Device> devices = getAll(db);
      // 修改表名
      db.execSQL("alter table " + tableName + " rename to tempTable");
      // 新建新表
      onCreate(db);
      // 将数据搬移至新表
      for (Device device : devices) db.insert(tableName, null, getValues(device));
      // 删除旧表
      db.execSQL("drop table tempTable");
    }
  }

  // 读取数据库设备列表
  @SuppressLint("Range")
  public ArrayList<Device> getAll() {
    return getAll(getReadableDatabase());
  }

  private ArrayList<Device> getAll(SQLiteDatabase db) {
    ArrayList<Device> devices = new ArrayList<>();
    try (Cursor cursor = db.query(tableName, null, null, null, null, null, null)) {
      if (cursor.moveToFirst()) do devices.add(getDeviceFormCursor(cursor)); while (cursor.moveToNext());
    }
    return devices;
  }

  // 查找
  @SuppressLint("Range")
  public Device getByUUID(String uuid) {
    Device device = null;
    try (Cursor cursor = getReadableDatabase().query(tableName, null, "uuid=?", new String[]{uuid}, null, null, null)) {
      if (cursor.moveToFirst()) device = getDeviceFormCursor(cursor);
    }
    return device;
  }

  // 更新
  public void insert(Device device) {
    getWritableDatabase().insert(tableName, null, getValues(device));
  }

  // 更新
  public void update(Device device) {
    getWritableDatabase().update(tableName, getValues(device), "uuid=?", new String[]{String.valueOf(device.uuid)});
  }

  // 删除
  public void delete(Device device) {
    getWritableDatabase().delete(tableName, "uuid=?", new String[]{String.valueOf(device.uuid)});
  }

  private ContentValues getValues(Device device) {
    ContentValues values = new ContentValues();
    values.put("uuid", device.uuid);
    values.put("type", device.type);
    values.put("name", device.name);
    values.put("isAudio", device.isAudio);
    values.put("address", device.address);
    values.put("specified_app", device.specified_app);
    values.put("maxSize", device.maxSize);
    values.put("maxFps", device.maxFps);
    values.put("maxVideoBit", device.maxVideoBit);
    values.put("setResolution", device.setResolution);
    values.put("defaultFull", device.defaultFull);
    values.put("useH265", device.useH265);
    values.put("useOpus", device.useOpus);
    values.put("connectOnStart", device.connectOnStart);
    values.put("clipboardSync", device.clipboardSync);
    values.put("nightModeSync", device.nightModeSync);
    return values;
  }

  @SuppressLint("Range")
  private Device getDeviceFormCursor(Cursor cursor) {
    return new Device(
      cursor.getString(cursor.getColumnIndex("uuid")),
      cursor.getInt(cursor.getColumnIndex("type")),
      cursor.getString(cursor.getColumnIndex("name")),
      cursor.getString(cursor.getColumnIndex("address")),
      cursor.getColumnIndex("specified_app") == -1 ? "" : cursor.getString(cursor.getColumnIndex("specified_app")),
      cursor.getInt(cursor.getColumnIndex("isAudio")) == 1,
      cursor.getInt(cursor.getColumnIndex("maxSize")),
      cursor.getInt(cursor.getColumnIndex("maxFps")),
      cursor.getInt(cursor.getColumnIndex("maxVideoBit")),
      cursor.getInt(cursor.getColumnIndex("setResolution")) == 1,
      cursor.getInt(cursor.getColumnIndex("defaultFull")) == 1,
      cursor.getColumnIndex("useH265") == -1 ? AppData.setting.getDefaultUseH265() : cursor.getInt(cursor.getColumnIndex("useH265")) == 1,
      cursor.getColumnIndex("useOpus") == -1 ? AppData.setting.getDefaultUseOpus() : cursor.getInt(cursor.getColumnIndex("useOpus")) == 1,
      cursor.getColumnIndex("connectOnStart") != -1 && cursor.getInt(cursor.getColumnIndex("connectOnStart")) == 1,
      cursor.getColumnIndex("clipboardSync") == -1 ? AppData.setting.getDefaultClipboardSync() : cursor.getInt(cursor.getColumnIndex("clipboardSync")) == 1,
      cursor.getColumnIndex("nightModeSync") == -1 ? AppData.setting.getDefaultNightModeSync() : cursor.getInt(cursor.getColumnIndex("nightModeSync")) == 1
    );
  }
}
