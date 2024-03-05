/*
 * 本项目大量借鉴学习了开源投屏软件：Scrcpy，在此对该项目表示感谢
 */
package top.eiyooooo.easycontrol.server.entity;

import android.util.Pair;

public final class DisplayInfo {
  public final int displayId;
  public final Pair<Integer, Integer> size;
  public int density;
  public final int rotation;
  public final int layerStack;
  public final int flags;

  public DisplayInfo(int displayId, Pair<Integer, Integer> size, int density, int rotation, int layerStack, int flags) {
    this.displayId = displayId;
    this.size = size;
    this.density = density;
    this.rotation = rotation;
    this.layerStack = layerStack;
    this.flags = flags;
  }
}
