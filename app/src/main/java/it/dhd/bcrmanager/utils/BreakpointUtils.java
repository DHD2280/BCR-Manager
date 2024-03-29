package it.dhd.bcrmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.dhd.bcrmanager.objects.Breakpoints;

public class BreakpointUtils {

    private static SharedPreferences getSharedPreferences(Context c, String prefName) {
        // Use your application context to get SharedPreferences
        return c.getSharedPreferences(prefName, Context.MODE_PRIVATE);
    }

    public static Map<String, Map<String, String>> loadBreakpoints(Context c, String prefName) {
        SharedPreferences preferences = getSharedPreferences(c, prefName);
        String breakpointsJson = preferences.getString(prefName, "{}");
        Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
        return new Gson().fromJson(breakpointsJson, type);
    }

    public static List<Breakpoints> loadListBreakpoints(Context c, String prefName) {
        Map<String, Map<String, String>> breakpoints = BreakpointUtils.loadBreakpoints(c, prefName);
        List<Breakpoints> breakpointsList = new ArrayList<>();
        // Iterate through the breakpoints and do something with them
        for (Map.Entry<String, Map<String, String>> entry : breakpoints.entrySet()) {
            String key = entry.getKey();
            Map<String, String> breakpoint = entry.getValue();

            // Extract values from the breakpoint
            String time = breakpoint.get("time");
            String title = breakpoint.get("title");
            String description = breakpoint.get("description");
            Log.d("BreakPointsFragment", "time: " + time + " title: " + title + " description: " + description);
            // Do something with the values, e.g., add them to a list
            // breakpointList.add(new Breakpoint(key, time, title, description));
            if (time != null) breakpointsList.add(new Breakpoints(key, Integer.parseInt(time), title, description));
        }
        return breakpointsList;
    }

    public static void saveBreakpoints(Context c, String prefName, Map<String, Map<String, String>> breakpoints) {
        String breakpointsJson = new Gson().toJson(breakpoints);
        SharedPreferences.Editor editor = getSharedPreferences(c, prefName).edit();
        editor.putString(prefName, breakpointsJson);
        editor.apply();
    }

}
