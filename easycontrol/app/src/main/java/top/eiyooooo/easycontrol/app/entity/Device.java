package top.eiyooooo.easycontrol.app.entity;

public class Device {
  public static final int TYPE_NORMAL = 1;
  public static final int TYPE_LINK = 2;

  public final String uuid;
  public final int type;
  public String name;
  public String address;
  public String specified_app;
  public boolean isAudio;
  public int maxSize;
  public int maxFps;
  public int maxVideoBit;
  public boolean setResolution;
  public boolean defaultFull;
  public boolean useH265;
  public boolean useOpus;
  public boolean connectOnStart;
  public int small_p_p_x;
  public int small_p_p_y;
  public int small_p_p_width;
  public int small_p_p_height;
  public int small_p_l_x;
  public int small_p_l_y;
  public int small_p_l_width;
  public int small_p_l_height;
  public int small_l_p_x;
  public int small_l_p_y;
  public int small_l_p_width;
  public int small_l_p_height;
  public int small_l_l_x;
  public int small_l_l_y;
  public int small_l_l_width;
  public int small_l_l_height;
  public int small_free_x;
  public int small_free_y;
  public int small_free_width;
  public int small_free_height;
  public static int SMALL_X = 0;
  public static int SMALL_Y = 0;
  public static int SMALL_WIDTH = 0;
  public static int SMALL_HEIGHT = 0;
  public int mini_y;
  public static int MINI_Y =200;
  public int connection = -1; // -1:未检查连接 0:检查连接中 1:可以连接 2:无法连接

  public Device(String uuid,
                int type,
                String name,
                String address,
                String specified_app,
                boolean isAudio,
                int maxSize,
                int maxFps,
                int maxVideoBit,
                boolean setResolution,
                boolean defaultFull,
                boolean useH265,
                boolean useOpus,
                boolean connectOnStart,
                int small_p_p_x, int small_p_p_y, int small_p_p_width, int small_p_p_height,
                int small_p_l_x, int small_p_l_y, int small_p_l_width, int small_p_l_height,
                int small_l_p_x, int small_l_p_y, int small_l_p_width, int small_l_p_height,
                int small_l_l_x, int small_l_l_y, int small_l_l_width, int small_l_l_height,
                int small_free_x, int small_free_y, int small_free_width, int small_free_height,
                int mini_y) {
    this.uuid = uuid;
    this.type = type;
    this.name = name;
    this.address = address;
    this.specified_app = specified_app;
    this.isAudio = isAudio;
    this.maxSize = maxSize;
    this.maxFps = maxFps;
    this.maxVideoBit = maxVideoBit;
    this.setResolution = setResolution;
    this.defaultFull = defaultFull;
    this.useH265 = useH265;
    this.useOpus = useOpus;
    this.connectOnStart = connectOnStart;
    this.small_p_p_x = small_p_p_x;
    this.small_p_p_y = small_p_p_y;
    this.small_p_p_width = small_p_p_width;
    this.small_p_p_height = small_p_p_height;
    this.small_p_l_x = small_p_l_x;
    this.small_p_l_y = small_p_l_y;
    this.small_p_l_width = small_p_l_width;
    this.small_p_l_height = small_p_l_height;
    this.small_l_p_x = small_l_p_x;
    this.small_l_p_y = small_l_p_y;
    this.small_l_p_width = small_l_p_width;
    this.small_l_p_height = small_l_p_height;
    this.small_l_l_x = small_l_l_x;
    this.small_l_l_y = small_l_l_y;
    this.small_l_l_width = small_l_l_width;
    this.small_l_l_height = small_l_l_height;
    this.small_free_x = small_free_x;
    this.small_free_y = small_free_y;
    this.small_free_width = small_free_width;
    this.small_free_height = small_free_height;
    this.mini_y = mini_y;
  }

  public Device(String uuid, int type) {
    this.uuid = uuid;
    this.type = type;
  }

  public static void copyDevice(Device source, Device target) {
    target.name = source.name;
    target.address = source.address;
    target.specified_app = source.specified_app;
    target.isAudio = source.isAudio;
    target.maxSize = source.maxSize;
    target.maxFps = source.maxFps;
    target.maxVideoBit = source.maxVideoBit;
    target.setResolution = source.setResolution;
    target.defaultFull = source.defaultFull;
    target.useH265 = source.useH265;
    target.useOpus = source.useOpus;
    target.connectOnStart = source.connectOnStart;
    target.small_p_p_x = source.small_p_p_x;
    target.small_p_p_y = source.small_p_p_y;
    target.small_p_p_width = source.small_p_p_width;
    target.small_p_p_height = source.small_p_p_height;
    target.small_p_l_x = source.small_p_l_x;
    target.small_p_l_y = source.small_p_l_y;
    target.small_p_l_width = source.small_p_l_width;
    target.small_p_l_height = source.small_p_l_height;
    target.small_l_p_x = source.small_l_p_x;
    target.small_l_p_y = source.small_l_p_y;
    target.small_l_p_width = source.small_l_p_width;
    target.small_l_p_height = source.small_l_p_height;
    target.small_l_l_x = source.small_l_l_x;
    target.small_l_l_y = source.small_l_l_y;
    target.small_l_l_width = source.small_l_l_width;
    target.small_l_l_height = source.small_l_l_height;
    target.small_free_x = source.small_free_x;
    target.small_free_y = source.small_free_y;
    target.small_free_width = source.small_free_width;
    target.small_free_height = source.small_free_height;
    target.mini_y = source.mini_y;
  }

  public static Device getDefaultDevice(String uuid, int type) {
    return new Device(uuid, type, uuid, "", "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultMaxVideoBit(), AppData.setting.getDefaultSetResolution(), AppData.setting.getDefaultFull(), AppData.setting.getDefaultUseH265(), AppData.setting.getDefaultUseOpus(), false, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT, SMALL_X, SMALL_Y, SMALL_WIDTH, SMALL_HEIGHT,MINI_Y);
  }

  public boolean isNormalDevice() {
    return type == TYPE_NORMAL;
  }

  public boolean isLinkDevice() {
    return type == TYPE_LINK;
  }

}
