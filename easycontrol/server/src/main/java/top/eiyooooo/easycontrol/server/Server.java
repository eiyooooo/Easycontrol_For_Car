package top.eiyooooo.easycontrol.server;

import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.view.Display;
import org.json.JSONArray;
import org.json.JSONObject;
import top.eiyooooo.easycontrol.server.entity.DisplayInfo;
import top.eiyooooo.easycontrol.server.utils.L;
import top.eiyooooo.easycontrol.server.utils.Workarounds;
import top.eiyooooo.easycontrol.server.wrappers.DisplayManager;
import top.eiyooooo.easycontrol.server.wrappers.ServiceManager;

import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class Server {
    Channel channel;
    public static DataOutputStream outputStream;

    public static void main(String... args) throws Exception {
        L.logMode = 2;
        ServiceManager.setManagers();
        Workarounds.prepareMainLooper();
        outputStream = new DataOutputStream(System.out);
        new Server();
        while (true) {
            Thread.sleep(1000);
        }
    }

    public Server() {
        inputHandler();
        this.channel = new Channel();
    }

    private void inputHandler() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    try {
                        String input = scanner.nextLine();
                        L.d("INPUT: " + input);
                        if (input.startsWith("/exit")) System.exit(0);
                        else if (input.startsWith("/")) handleRequest(parseRequest(input));
                        else throw new Exception("Unknown command");
                    } catch (Exception e) {
                        L.e("consoleInputHandler error", e);
                    }
                }
            }
        }).start();
    }

    private void postResponse(String response) {
        try {
            if (response == null) response = "null";
            L.d("RESPONSE: " + response);
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            outputStream.writeInt(responseBytes.length);
            outputStream.write(responseBytes);
        } catch (Exception e) {
            L.e("postResponse error", e);
        }
    }

    private static HashMap<String, String> parseRequest(String input) {
        HashMap<String, String> request = new HashMap<>();

        String[] parts = input.split("\\?");
        String path = parts[0];
        request.put("request", path);
        if (parts.length == 1) return request;

        String params = parts[1];
        String[] keyValuePairs = params.split("&");
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length != 2) continue;
            String key = keyValue[0];
            String value = keyValue[1];
            request.put(key, value);
        }
        return request;
    }

    Map<Integer, VirtualDisplay> cache = new HashMap<>();

    private void handleRequest(HashMap<String, String> request) {
        try {
            switch (Objects.requireNonNull(request.get("request"))) {
                case "/getPhoneInfo": {
                    postResponse(Channel.getPhoneInfo().toString());
                    break;
                }
                case "/getRecentTasks": {
                    String line1 = request.get("maxNum");
                    String line2 = request.get("flags");
                    String line3 = request.get("userId");

                    int maxNum = 25;
                    if (line1 != null) maxNum = Integer.parseInt(line1);
                    int flags = 0;
                    if (line2 != null) flags = Integer.parseInt(line2);
                    int userId = 0;
                    if (line3 != null) userId = Integer.parseInt(line3);

                    postResponse(channel.getRecentTasksJson(maxNum, flags, userId).toString());
                    break;
                }
                case "/getIcon": {
                    String packageName = request.get("package");
                    if (packageName == null) throw new Exception("parameter 'package' not found");
                    postResponse(channel.Bitmap2file(packageName));
                    break;
                }
                case "/getAllAppInfo": {
                    String line = request.get("app_type");
                    if (line == null) throw new Exception("parameter 'app_type' not found");
                    int appType = Integer.parseInt(line);
                    postResponse(channel.getAllAppInfo(appType));
                    break;
                }
                case "/getAppDetail": {
                    String packageName = request.get("package");
                    if (packageName == null) throw new Exception("parameter 'package' not found");
                    postResponse(channel.getAppDetail(packageName));
                    break;
                }
                case "/getAppMainActivity": {
                    String packageName = request.get("package");
                    if (packageName == null) throw new Exception("parameter 'package' not found");
                    postResponse(channel.getAppMainActivity(packageName));
                    break;
                }
                case "/createVirtualDisplay": {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                        throw new Exception("Virtual display is not supported before Android 11");
                    String line1 = request.get("width");
                    String line2 = request.get("height");
                    String line3 = request.get("density");
                    if (line1 != null && line2 == null)
                        throw new Exception("parameter 'width' found, but 'height' not found");
                    if (line1 == null && line2 != null)
                        throw new Exception("parameter 'height' found, but 'width' not found");

                    DisplayInfo defaultDisplay = DisplayManager.getDisplayInfo(Display.DEFAULT_DISPLAY);
                    int width, height, density;
                    if (line1 != null) {
                        width = Integer.parseInt(line1);
                        height = Integer.parseInt(line2);
                    } else {
                        int rotation = defaultDisplay.rotation;
                        if (rotation == 1 || rotation == 3) {
                            width = defaultDisplay.size.second;
                            height = defaultDisplay.size.first;
                        } else {
                            width = defaultDisplay.size.first;
                            height = defaultDisplay.size.second;
                        }
                    }
                    if (line3 != null) density = Integer.parseInt(line3);
                    else density = defaultDisplay.density;

                    VirtualDisplay display = channel.createVirtualDisplay(width, height, density);
                    if (display == null) throw new Exception("Failed to create virtual display");
                    int createdDisplayId = display.getDisplay().getDisplayId();
                    cache.put(createdDisplayId, display);
                    int[] displayIds = DisplayManager.getDisplayIds();
                    for (int displayId : displayIds) {
                        L.d(">>>display -> " + displayId);
                    }
                    postResponse("success create display, id -> " + createdDisplayId);
                    break;
                }
                case "/resizeDisplay": {
                    String line1 = request.get("id");
                    String line2 = request.get("width");
                    String line3 = request.get("height");
                    String line4 = request.get("density");
                    int id = 0;
                    if (line1 != null) id = Integer.parseInt(line1);
                    DisplayInfo display = DisplayManager.getDisplayInfo(id);
                    if (display == null) throw new Exception("specified display not found");

                    if (line2 == null && line3 == null && line4 == null)
                        throw new Exception("please give parameter 'width'&'height' or 'density'");
                    if (line2 != null && line3 == null)
                        throw new Exception("parameter 'width' found, but 'height' not found");
                    if (line2 == null && line3 != null)
                        throw new Exception("parameter 'height' found, but 'width' not found");

                    int width, height, density;
                    if (line2 != null) {
                        width = Integer.parseInt(line2);
                        height = Integer.parseInt(line3);
                    } else {
                        width = display.size.first;
                        height = display.size.second;
                    }
                    if (line4 != null) density = Integer.parseInt(line4);
                    else density = display.density;

                    if (id == 0) {
                        if (line2 != null) Channel.execReadOutput("wm size " + width + "x" + height);
                        if (line4 != null) Channel.execReadOutput("wm density " + density);
                    } else {
                        VirtualDisplay virtualDisplay = cache.get(id);
                        if (virtualDisplay == null)
                            throw new Exception("specified virtual display not found, it might not be created by this server");
                        virtualDisplay.resize(width, height, density);
                    }
                    postResponse("success resize display, id -> " + id);
                    break;
                }
                case "/releaseVirtualDisplay": {
                    String id = request.get("id");
                    if (id == null) throw new Exception("parameter 'id' not found");
                    VirtualDisplay display = cache.get(Integer.parseInt(id));
                    if (display == null)
                        throw new Exception("specified virtual display not found, it might not be created by this server");
                    JSONObject tasks = channel.getRecentTasksJson(25, 0, 0);
                    JSONArray tasks_data = tasks.getJSONArray("data");
                    for (int i = 0; i < tasks.length(); i++) {
                        JSONObject task = tasks_data.getJSONObject(i);
                        if (id.equals(String.valueOf(task.getInt("displayId")))) {
                            try {
                                Channel.execReadOutput("am display move-stack " + task.getInt("id") + " 0");
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    display.release();
                    cache.remove(Integer.parseInt(id));
                    postResponse("success release display, id -> " + id);
                    break;
                }
                case "/openAppByPackage": {
                    String packageName = request.get("package");
                    String activity = request.get("activity");
                    String id = request.get("displayId");

                    if (packageName == null) throw new Exception("parameter 'package' not found");
                    if (activity == null) activity = channel.getAppMainActivity(packageName);
                    if (id == null) id = "0";

                    String error = channel.openApp(packageName, activity, Integer.parseInt(id));
                    if (error != null) throw new Exception(error);
                    postResponse("success");
                    break;
                }
                case "/stopAppByPackage": {
                    String packageName = request.get("package");
                    if (packageName == null) throw new Exception("parameter 'package' not found");
                    String cmd = "am force-stop " + packageName;
                    L.d("stopActivity activity cmd: " + cmd);
                    Channel.execReadOutput(cmd);
                    postResponse("success");
                    break;
                }
                case "/getDisplayInfo": {
                    int[] displayIds = DisplayManager.getDisplayIds();
                    JSONArray jsonArray = new JSONArray();
                    for (int displayId : displayIds) {
                        DisplayInfo display = DisplayManager.getDisplayInfo(displayId);
                        if (display == null) continue;
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("id", displayId);
                        jsonObject.put("width", display.size.first);
                        jsonObject.put("height", display.size.second);
                        jsonObject.put("density", display.density);
                        jsonObject.put("rotation", display.rotation);
                        jsonArray.put(jsonObject);
                    }
                    postResponse(jsonArray.toString());
                    break;
                }
                case "/runShell": {
                    String cmd = request.get("cmd");
                    if (cmd == null) throw new Exception("parameter 'cmd' not found");
                    L.d("runShell cmd: " + cmd);
                    postResponse(Channel.execReadOutput(cmd));
                    break;
                }
                default:
                    postResponse("Unknown request");
            }
        } catch (Exception e) {
            postResponse(e.getMessage());
            L.e("handleRequest error", e);
        }
    }
}
