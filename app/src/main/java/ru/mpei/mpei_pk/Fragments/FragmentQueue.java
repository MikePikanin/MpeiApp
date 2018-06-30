package ru.mpei.mpei_pk.Fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;

import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimerTask;
import java.util.TreeSet;

import ru.mpei.mpei_pk.ProtocolMPEI;
import ru.mpei.mpei_pk.R;
import ru.mpei.mpei_pk.activities.MainActivity;

import static ru.mpei.mpei_pk.activities.MainActivity.timer;


public class FragmentQueue extends Fragment {

    private ProtocolMPEI protocolMPEI;
    private Context context;

    private String VisitType;
    private String EducationLevel;
    private String OnlyPayForm;

    private MyTimerTask timerTask;
    private static boolean runTimer = true;

    public FragmentQueue() {
        // Required empty public constructor
    }

    public static FragmentQueue newInstance() {//int VisitType, int EducationLevel) {
        return new FragmentQueue();
    }


    @Override
    public void onDestroy() {
        runTimer = false;
        try {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
        } catch (Exception e) {
            Log.e("FragmentQueue", e.getMessage());
        }
        super.onDestroy();
    }
    @Override
    public void onPause() {
        runTimer = false;
        super.onPause();
    }

    @Override
    public void onResume() {
        runTimer = true;
        super.onResume();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        protocolMPEI = new ProtocolMPEI(context);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try {
            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NavigationView navigation = (NavigationView) ((Activity)context).findViewById(R.id.nav_view);
                    navigation.getMenu().getItem(1).setChecked(true);
                    ProgressBar progressBar = (ProgressBar) ((MainActivity) context).findViewById(R.id.progressBarMain);
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });
            timerTask = new MyTimerTask();
            new MyThread().start();
        } catch (Exception e) {
            Log.e("FragmentQueue", e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_queue_reserve, container, false);
    }

    @Override
    public void onAttach(Context context) {
        this.context = context;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private class MyThread extends Thread {
        @Override
        public void run() {
            try {
                HashMap<String, String> map = protocolMPEI.get_person_info();
                if (map != null) {
                    if (map.containsKey("error")) {
                        try {
                            final String error = map.get("error");
                            drawError(error);
                            ((MainActivity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ProgressBar progressBar = (ProgressBar) ((MainActivity) context).findViewById(R.id.progressBarMain);
                                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                                }
                            });
                        } catch (Exception e) {
                            Log.e("FragmentQueue", e.getMessage());
                        }
                    } else {
                        VisitType = map.get("VisitType");
                        EducationLevel = map.get("EducationLevel");
                        OnlyPayForm = map.get("OnlyPayForm");
                        final String idReserve = map.get("IdReserve");
                        int id_reserve;
                        try {
                            id_reserve = Integer.parseInt(idReserve);
                        } catch (Exception e) {
                            id_reserve = 0;
                        }
                        if (id_reserve > 0 && map.get("ReserveStatus").equals("OK")) {
                            String txtInterval = map.get("ReserveInterval");
                            String txtEduLevel = map.get("ReserveEducationLevel");
                            String txtVisitType = map.get("ReserveVisitType");
                            final String reservedInfo = "Вами зарезервировано место в очереди на " + txtVisitType + " (" + txtEduLevel + ") " + txtInterval + ".";
                            drawCancelReserve(idReserve, reservedInfo);
                            ((MainActivity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ProgressBar progressBar = (ProgressBar) ((MainActivity) context).findViewById(R.id.progressBarMain);
                                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                                }
                            });
                        } else {
                            final String answer;
                            if (VisitType != null && EducationLevel != null && OnlyPayForm != null) {
                                answer = protocolMPEI.get_reserve_room(VisitType, EducationLevel, OnlyPayForm);
                            } else {
                                answer = null;
                            }
                            if (answer != null) {
                                drawTable(answer);
                            } else {
                                drawError(null);
                            }
                            //Сокрытие полосы загрузки.
                            ((MainActivity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ProgressBar progressBar = (ProgressBar) ((MainActivity) context).findViewById(R.id.progressBarMain);
                                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                                }
                            });
                            timer.schedule(timerTask, 30000, 30000);
                        }
                    }
                } else {
                    drawError(null);
                    ((MainActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ProgressBar progressBar = (ProgressBar) ((MainActivity) context).findViewById(R.id.progressBarMain);
                            progressBar.setVisibility(ProgressBar.INVISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("FragmentQueue", e.getMessage());
            }
        }
    }

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            if (runTimer) {
                try {
                    String s = protocolMPEI.get_reserve_room(VisitType, EducationLevel, OnlyPayForm);
                    if (s != null) {
                        drawTable(s);
                    } else {
                        drawError(null);
                    }
                } catch (Exception e) {
                    Log.e("FragmentQueueTimerTask", e.getMessage());
                }
            } else {
                this.cancel();
            }
        }
    }
    private void drawError(String error) {
        try {
            if (error == null) {
                error = "\nВ данный момент резервирование невозможно, попробуйте позднее.";
            }
            final ArrayList<View> tableRowList = new ArrayList<>();

            TableRow tableRow;
            TextView textView;

            //header
            tableRow = new TableRow(context);
            textView = new TextView(context);
            textView.setText(error);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            textView.setGravity(Gravity.CENTER);
            tableRow.addView(textView);
            tableRowList.add(tableRow);

            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TableLayout table = ((Activity)context).findViewById(R.id.queue_reserve_table);
                    table.removeAllViews();
                    table.setStretchAllColumns(true);
                    table.setShrinkAllColumns(true);
                    for (View v : tableRowList) {
                        table.addView(v);
                    }
                }
            });
        }
        catch (Exception e) {
            Log.e("FragmentQueue", e.getMessage());
        }
    }

    private void drawCancelReserve(final String id_reserve, String reservedInfo) {
        try {

            TableRow.LayoutParams rowParams = new TableRow.LayoutParams();
            rowParams.span = 2;

            final ArrayList<View> tableRowList = new ArrayList<>();
            TableRow tableRow;
            TextView textView;
            Button button;

            tableRow = new TableRow(context);
            textView = new TextView(context);
            textView.setText(reservedInfo);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(rowParams);
            tableRow.addView(textView);
            tableRowList.add(tableRow);

            tableRow = new TableRow(context);
            textView = new TextView(context);
            textView.setText("Отменить резерв");
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            textView.setGravity(Gravity.CENTER);
            tableRow.addView(textView);
            button = new Button(context);
            button.setText("Отменить");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                timerTask = new MyTimerTask();
                                ((MainActivity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ProgressBar progressBar = ((MainActivity) context).findViewById(R.id.progressBarMain);
                                        progressBar.setVisibility(ProgressBar.VISIBLE);
                                    }
                                });

                                String s = protocolMPEI.queue_reserve(id_reserve, 1);
                                String answer = null;
                                final String message;
                                if (s != null) {
                                    if (s.equals("REJECTED")) {
                                        message = "Резерв отменен";
                                    } else {
                                        message = "Резерв уже был отменен";
                                    }
                                    if (VisitType != null && EducationLevel != null && OnlyPayForm != null) {
                                        answer = protocolMPEI.get_reserve_room(VisitType, EducationLevel, OnlyPayForm);
                                    }
                                } else {
                                    message = "Ошибка, попробуйте позднее";
                                }
                                if (answer != null) {
                                    drawTable(answer);
                                } else {
                                    new MyThread().start();
                                }
                                ((MainActivity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                        ProgressBar progressBar = ((MainActivity) context).findViewById(R.id.progressBarMain);
                                        progressBar.setVisibility(ProgressBar.INVISIBLE);
                                    }
                                });
                                runTimer = true;
                            } catch (Exception e) {
                                Log.e("FragmentQueue", e.getMessage());
                            }
                        }
                    }).start();
                }
            });
            button.setGravity(Gravity.CENTER);
            tableRow.addView(button);
            tableRowList.add(tableRow);

            tableRow = new TableRow(context);
            textView = new TextView(context);
            textView.setText(getResources().getString(R.string.queue_reserved_info));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            textView.setLayoutParams(rowParams);
            tableRow.addView(textView);
            tableRowList.add(tableRow);

            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TableLayout table = ((Activity)context).findViewById(R.id.queue_reserve_table);
                    table.removeAllViews();
                    table.setStretchAllColumns(true);
                    table.setShrinkAllColumns(true);
                    for (View v : tableRowList) {
                        table.addView(v);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("FragmentQueue", e.getMessage());
        }
    }

    private void drawTable(String s) {
        try {
            JSONObject jsonObj = new JSONObject(s);
            int has_intervals = jsonObj.getInt("has_intervals");
            JSONObject rooms = jsonObj.getJSONObject("room");
            if (has_intervals == 1) {
                JSONObject jo_int = jsonObj.getJSONObject("intervals");
                JSONArray intervals = jo_int.toJSONArray(jo_int.names());
                TreeSet<String> tree = new TreeSet<>();
                for(int i = 0; i < intervals.length(); i++) {
                    tree.add(intervals.getJSONObject(i).getString("IdTimeInterval"));
                }

                TreeSet<String> dates = new TreeSet<>();

                Calendar calendar = Calendar.getInstance();
                Calendar now = Calendar.getInstance();
                int today = now.get(Calendar.DAY_OF_MONTH);
                int year = now.get(Calendar.YEAR);
                int hour = now.get(Calendar.HOUR_OF_DAY);

                DateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
                for(int i = 0; i < intervals.length(); i++) {
                    JSONObject jo = intervals.getJSONObject(i);
                    if (!dates.contains(jo.getString("Day"))) {
                        calendar.setTime(format.parse(jo.getString("Day")));
                        if (calendar.after(now) || (today == calendar.get(Calendar.DAY_OF_MONTH) && year == calendar.get(Calendar.YEAR) && hour < 18)) {
                            dates.add(jo.getString("Day"));
                        }
                    }
                }

                final ArrayList<View> tableRowList = new ArrayList<>();

                int minHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
                int border = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

                TableRow tableRow;
                TextView textView;
                Button button;
                View divider;

                TableRow.LayoutParams itemParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);

                TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT, border);

                TableRow.LayoutParams rowParams = new TableRow.LayoutParams();
                rowParams.span = 2;

                //header
                tableRow = new TableRow(context);
                tableRow.setBackground(getResources().getDrawable(R.drawable.queue_head_background));
                tableRow.setMinimumHeight(minHeight);
                tableRow.setGravity(Gravity.CENTER_VERTICAL);
                textView = new TextView(context);
                textView.setText("Резервирование очереди");
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                textView.setTextColor(getResources().getColor(R.color.colorBlack));
                textView.setLayoutParams(rowParams);
                textView.setGravity(Gravity.CENTER);
                tableRow.addView(textView);
                tableRowList.add(tableRow);

                int i = 0;
                for (String date : dates) {
                    //Граница
                    divider = new View(context);
                    divider.setLayoutParams(tableParams);
                    divider.setBackgroundColor(getResources().getColor(R.color.colorBlack));
                    tableRowList.add(divider);
                    //Дата
                    tableRow = new TableRow(context);
                    tableRow.setGravity(Gravity.CENTER_VERTICAL);
                    tableRow.setBackground(getResources().getDrawable(R.drawable.queue_head_background));

                    textView = new TextView(context);
                    textView.setText(date);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
                    textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    textView.setLayoutParams(rowParams);
                    textView.setGravity(Gravity.CENTER);
                    tableRow.addView(textView);
                    tableRowList.add(tableRow);

                    //Граница
                    divider = new View(context);
                    divider.setLayoutParams(tableParams);
                    divider.setBackgroundColor(getResources().getColor(R.color.colorBlack));
                    tableRowList.add(divider);

                    for(String x : tree) {
                        JSONObject jo = jo_int.getJSONObject(x);
                        if (jo.getString("Day").equals(date)) {
                            //Строка
                            tableRow = new TableRow(context);
                            tableRow.setMinimumHeight(minHeight);
                            tableRow.setGravity(Gravity.CENTER_VERTICAL);
                            //Первая колонка с интервалом
                            textView = new TextView(context);
                            final String time = "C " + jo.getString("BeginTime") + " до " + jo.getString("EndTime");
                            textView.setText(time);
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                            textView.setGravity(Gravity.CENTER);
                            textView.setLayoutParams(itemParams);
                            textView.setTextColor(getResources().getColor(R.color.colorBlack));
                            tableRow.addView(textView);

                            int count = 0;
                            if (rooms.has(jo.getString("IdTimeInterval"))) {
                                count = Integer.valueOf(rooms.getString(jo.getString("IdTimeInterval")));
                            }
                            //Если есть места, отображаем кнопку, иначе текст
                            if (count > 0) {
                                final String id_interval = jo.getString("IdTimeInterval");
                                button = new Button(context);
                                button.setText("Зарезервировать");
                                button.setLayoutParams(itemParams);
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((MainActivity) context).runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ProgressBar progressBar = ((MainActivity) context).findViewById(R.id.progressBarMain);
                                                        progressBar.setVisibility(ProgressBar.VISIBLE);
                                                    }
                                                });
                                                final String s = protocolMPEI.queue_reserve(id_interval, 0);
                                                final String message;
                                                String info = null;
                                                if (s != null) {
                                                    if (s.startsWith("RESERVED")) {
                                                        HashMap<String, String> map = protocolMPEI.get_person_info();
                                                        if (map != null) {
                                                            String txtStatus = map.get("ReserveStatus");
                                                            if (txtStatus.equals("OK")) {
                                                                timerTask.cancel();
                                                                runTimer = false;
                                                                message = "Очередь зарезервирована!";

                                                                String txtInterval = map.get("ReserveInterval");
                                                                String txtEduLevel = map.get("ReserveEducationLevel");
                                                                String txtVisitType = map.get("ReserveVisitType");
                                                                info = txtInterval + " " + txtEduLevel + " " + txtVisitType;
                                                            } else {
                                                                message = "Произошла ошибка, попробуйте позднее";
                                                                new MyThread().start();
                                                            }
                                                        } else {
                                                            message = "Произошла ошибка, попробуйте позднее";
                                                            new MyThread().start();
                                                        }
                                                    } else if (s.startsWith("OVERLOAD")) {
                                                        message = "К сожалению мест не осталось";
                                                    } else if (s.startsWith("ALREADY")) {
                                                        message = "Место уже было зарезивировано";
                                                    } else {
                                                        message = "Произошла ошибка, попробуйте позднее";
                                                        new MyThread().start();
                                                    }
                                                } else {
                                                    message = "Произошла ошибка, попробуйте позднее";
                                                    new MyThread().start();
                                                }
                                                if (s != null && s.startsWith("RESERVED") && info != null) {
                                                    drawCancelReserve(s.split("&")[1], info);
                                                }
                                                ((MainActivity) context).runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                                        ProgressBar progressBar = ((MainActivity) context).findViewById(R.id.progressBarMain);
                                                        progressBar.setVisibility(ProgressBar.INVISIBLE);
                                                    }
                                                });
                                            }
                                        }).start();
                                    }
                                });
                                button.setGravity(Gravity.CENTER);
                                tableRow.addView(button);
                                tableRow.setBackgroundColor(getResources().getColor(R.color.colorEmptyQueue));
                            }
                            else {
                                textView = new TextView(context);
                                textView.setText("Резервирование завершено");
                                textView.setGravity(Gravity.CENTER);
                                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                                textView.setTextColor(getResources().getColor(R.color.colorBlack));
                                textView.setLayoutParams(itemParams);
                                tableRow.addView(textView);
                                tableRow.setBackgroundColor(getResources().getColor(R.color.colorFullQueue));
                            }
                            //table.addView(tableRow);
                            tableRowList.add(tableRow);
                            //Граница
                            divider = new View(context);
                            divider.setLayoutParams(tableParams);
                            divider.setBackgroundColor(getResources().getColor(R.color.colorBlack));
                            //table.addView(divider);
                            tableRowList.add(divider);
                        }
                    }
                    //Разрыв между датами
                    if (++i != dates.size()) {
                        tableRow = new TableRow(context);
                        textView = new TextView(context);
                        tableRow.addView(textView);
                        tableRow.setBackgroundColor(Color.WHITE);
                        //table.addView(tableRow);
                        tableRowList.add(tableRow);
                    }
                }

                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TableLayout table = ((Activity)context).findViewById(R.id.queue_reserve_table);
                        table.removeAllViews();
                        table.setStretchAllColumns(true);
                        table.setShrinkAllColumns(true);
                        for (View v : tableRowList) {
                            table.addView(v);
                        }
                    }
                });

            }
        } catch (Exception e) {
            Log.e("FragmentQueue", e.getMessage());
        }
    }
}
