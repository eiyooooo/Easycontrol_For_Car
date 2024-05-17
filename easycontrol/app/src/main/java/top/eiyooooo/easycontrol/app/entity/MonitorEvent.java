package top.eiyooooo.easycontrol.app.entity;

public class MonitorEvent {
  public final String uuid;
  public final String packageName;
  public final String className;
  public final int eventType;
  /**
   * 1:change_to_mini
   * <p>
   * 2:change_to_small
   */
  public final int responseType;

  public MonitorEvent(String uuid, String packageName, String className, int eventType, int responseType) {
    this.uuid = uuid;
    this.packageName = packageName;
    this.className = className;
    this.eventType = eventType;
    this.responseType = responseType;
  }
}
