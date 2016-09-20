package mfec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static jdk.nashorn.internal.objects.NativeMath.round;

public class Salary {
    final static String startWorking = "8:00";
    final static String finishTimeWorking = "17:00";
    final static String startOverTime = "17:30";
    static List<Employee> employee_list = new ArrayList<Employee>();

    public static void main(String[] args) {
        //List<Employee> employee_list = new ArrayList<Employee>();
        List<String> raw_list = new ArrayList<String>();
        String path = "C:\\1.working_time.log";
        SimpleDateFormat timeparser = new SimpleDateFormat("HH:mm");
        String str = "";
        String[] part;
        // read file from path
        try {
            //	File file = new File(path);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(path), "UTF-8"));
            while ((str = in.readLine()) != null) {

                raw_list.add(str);
            }
            in.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }        
        ////Static bug - fix!first character first line -  ? fix - original file error
        if ((int) raw_list.get(0).charAt(0) == 65279) {
            String fxe = raw_list.get(0);
            StringBuilder sb = new StringBuilder(fxe);
            sb.deleteCharAt(0);
            str = sb.toString();
            raw_list.set(0, str);
        }
        //query from raw file
        for (String line : raw_list) {
            int index = 0;
            boolean isFound = false;

            part = line.split("\\|");
            //System.out.print(part[0]);
            String name = part[0];

            // Employee Exist?		    
            for (int i = 0; i < employee_list.size(); i++) {
                if (employee_list.get(i).getName().equals(name)) {
                    index = i;
                    isFound = true;
                    break;
                }
            }
            if (isFound) {
                // Start work ?
                if (part[2].length() >= 3) {
                    workingState(index, line);
                }
            } else {
                employee_list.add(new Employee(part[0]));
                // Start work ?
                if (part[2].length() >= 3) {
                    workingState(employee_list.size() - 1, line);
                }
            }
        }
        int count = 1;
        for (Employee emp : employee_list) {
            System.out.println(count + " " + emp.getName());            
            balanceCalculator(emp.getWorkingHour());
            count++;
        }
    }

    static public void workingState(int emp_id, String line) {
        String[] part;
        part = line.split("\\|");
        String name = part[0];
        String dateIn = part[1];
        String timeIn = part[2];
        String dateOut = part[3];
        String timeOut = part[4];
        Double working_Hour = 1.0;
        Double dayWorked ;
        if(isWeekend(dateIn)){
            working_Hour += 0.5;
        }
        // isSunday or Saturday?
        
            //ONTIME
            int earlyMorning = overTimeDurationCount(startWorking,timeIn);
            if (!isLate(timeIn) && earlyMorning >= 0) {
                //Punch-Out Time Check                
                if ("NORMAL".equals(checkOutType(timeOut, finishTimeWorking))) {
                    dayWorked = (8*60)*working_Hour;
                    employee_list.get(emp_id).increaseworkingHour(dayWorked);
                    //System.out.println(dayWorked);
                } else if ("EARLY".equals(checkOutType(timeOut, finishTimeWorking)  )) {
                    //half-day morning work
                    if ("NORMAL".equals(checkOutType(timeOut, "12:00"))) { 
                        dayWorked = (4*60)*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                        //System.out.println(dayWorked);
                    }
                } else if ("OVER".equals(checkOutType(timeOut, finishTimeWorking))) {
                    //DAY-Time Workinghour
                    dayWorked = (8*60)*working_Hour;
                    employee_list.get(emp_id).increaseworkingHour(dayWorked);
                    //System.out.print(dayWorked + "  OT:");
                    //OVER-Time Workinghour
                    working_Hour += 0.5;
                    int ots = overTimeDurationCount(timeOut, startOverTime);
                        //round down
                        ots /= 10;
                        ots *= 10;
                    dayWorked = (ots)*working_Hour;
                   // System.out.println(ots);
                    //NORMAL_DAY AND OT
                    employee_list.get(emp_id).increaseworkingHour(dayWorked);
                }else {System.out.println("*******");}
            } else {
              //LATE OR OverTime
                //MORNING Punch-In Late && NORMAL Punch-Out
                int before_noon_duration = overTimeDurationCount("13:00", timeIn);
                //isNOT OT and before 12:00
                if (before_noon_duration > 60) {
                    
                    if ("NORMAL".equals(checkOutType(timeOut, finishTimeWorking))) {
                        dayWorked = ((before_noon_duration-60) + (4 * 60))*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                        //System.out.println(dayWorked);
                    } else if ("EARLY".equals(checkOutType(timeOut, finishTimeWorking))) {
                        //MORNING Punch-In Late && EARLY Punch-Out
                        dayWorked = (before_noon_duration-60) + ((overTimeDurationCount(timeOut, finishTimeWorking)/10)*10)*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                        //System.out.println(dayWorked);
                    } else if ("OVER".equals(checkOutType(timeOut, finishTimeWorking))) {
                        //LATE + AFTERNOON
                        dayWorked = (((before_noon_duration-60) + (4 * 60)))*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                        //System.out.print(dayWorked+" *");
                        //+OT
                        working_Hour += 0.5;
                        int ot = (overTimeDurationCount(timeOut,startOverTime)/10)*10;
                        dayWorked = (ot*working_Hour);
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                        //System.out.println(name +"__ "+ timeIn+" "+ot);
                    }else {System.out.println("***Something went wrong with time***");}
                }else if(before_noon_duration >=0 ){                    
                    //Noon Punch-In
                    if ("NORMAL".equals(checkOutType(timeOut, finishTimeWorking))) {
                        dayWorked = (4 * 60)*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                       // System.out.println(dayWorked);
                    }else if ("OVER".equals(checkOutType(timeOut, finishTimeWorking))) {
                        //AFTERNOON
                        dayWorked = (4 * 60)*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked); 
                       // System.out.print(dayWorked+"  ");
                        //+OT
                        working_Hour += 0.5;
                        dayWorked = ((overTimeDurationCount(timeOut,startOverTime)/10)*10)*working_Hour;
                      //  System.out.println(dayWorked);
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                    }
                    
                    
                }else {
                    //OverTime ****************
                    working_Hour += 0.5;
                    int overTimeNormalStart = overTimeDurationCount(startOverTime, timeIn);
                    if(overTimeNormalStart >= 0 && overTimeNormalStart <= 30){
                        //onTime 17:30 puncIn
                        double ot = countTime(dateIn, startOverTime, dateOut, timeOut);
                        dayWorked = (ot)*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                        //System.out.println(dayWorked);
                    }else {
                        //after 17:30 punchIn
                        double ot = countTime(dateIn, timeIn, dateOut, timeOut);
                        dayWorked = (ot)*working_Hour;
                        employee_list.get(emp_id).increaseworkingHour(dayWorked);
                       // System.out.println(dayWorked);
                    }                    
                }
            }
        
    }

    private static int countTime(String dateIn, String timein, String dateOut, String timeout) {
        int[] tempDateIn = convertToLocaleDate(dateIn);
        int[] tempTimeIn = convertToLocaleTime(timein);
        int[] tempDateOut = convertToLocaleDate(dateOut);
        int[] tempTimeOut = convertToLocaleTime(timeout);

        //format YY/MM/DD HH/MM
        LocalDateTime fromDateTime = LocalDateTime.of(tempDateIn[0], tempDateIn[1], tempDateIn[2], tempTimeIn[0], tempTimeIn[1]);
        LocalDateTime toDateTime = LocalDateTime.of(tempDateOut[0], tempDateOut[1], tempDateOut[2], tempTimeOut[0], tempTimeOut[1]);

        LocalDateTime tempDateTime = LocalDateTime.from(fromDateTime);

        long hours = tempDateTime.until(toDateTime, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);

        long minutes = tempDateTime.until(toDateTime, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes(minutes);

        // System.out.println(" "+hours + " hours " + minutes + " minutes ");
        long allTime = (hours * 60) + minutes;
        return (int) allTime;
    }

    public static int[] convertToLocaleDate(String raw) {
        String r[] = new String[3];
        int i[] = new int[3];
        r = raw.split("/");
        i[0] = Integer.parseInt(r[2]);
        i[1] = Integer.parseInt(r[1]);
        i[2] = Integer.parseInt(r[0]);

        return i;
    }

    public static int[] convertToLocaleTime(String raw) {
        String r[] = new String[2];
        int i[] = new int[2];
        r = raw.split(":");
        i[0] = Integer.parseInt(r[0]);
        i[1] = Integer.parseInt(r[1]);
        return i;
    }

    public static boolean isLate(String time) {
        int t[] = convertToLocaleTime(time);
        int userHour = t[0];
        int userMinute = t[1];
        int hourTime = 8;
        int minuteTime = 5;
        LocalDateTime startWorkingHour = LocalDateTime.of(2000, 2, 2, hourTime, minuteTime);
        LocalDateTime userStartWorking = LocalDateTime.of(2000, 2, 2, userHour, userMinute);
        if (minuteCheck(startWorkingHour, userStartWorking) <= 0) {
            return false;
        } else {
            return true;
        }
    }
    //Delete isOT
    private static int minuteCheck(LocalDateTime startWorkingHour, LocalDateTime userStartWorking) {

        LocalDateTime tempDateTime = LocalDateTime.from(startWorkingHour);

        long hours = tempDateTime.until(userStartWorking, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);

        long minutes = tempDateTime.until(userStartWorking, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes(minutes);
        minutes = minutes + (hours * 60);
        // System.out.println(" "+hours + " hours " + minutes + " minutes ");
        return (int) minutes;
    }

    private static boolean isWeekend(String part) {
        int[] tempDateIn = convertToLocaleDate(part);
        LocalDateTime fromDateTime = LocalDateTime.of(tempDateIn[0], tempDateIn[1], tempDateIn[2], 12, 00);
        DayOfWeek day = fromDateTime.getDayOfWeek();

        if (day.toString().equals("SATURDAY") || day.toString().equals("SUNDAY")) {
            return true;
        } else {
            return false;
        }
    }

    private static String checkOutType(String timeOut, String lineTime) {
        int[] tempTimeIn = convertToLocaleTime(timeOut);
        String r[] = new String[2];
        String l[] = new String[2];
        r = timeOut.split(":");
        l = lineTime.split(":");
        //LocalDateTime fromDateTime = LocalDateTime.of(2015, 5, 5, tempTimeIn[0], tempTimeIn[1]);
        //LocalDateTime toDateTime = LocalDateTime.of(2015, 5, 5, 17, 00);
        LocalDateTime startWorkingHour = LocalDateTime.of(2000, 2, 2, Integer.parseInt(r[0]), Integer.parseInt(r[1]));
        LocalDateTime userStartWorking = LocalDateTime.of(2000, 2, 2, Integer.parseInt(l[0]), Integer.parseInt(l[1]));
        int diffTime = minuteCheck(userStartWorking, startWorkingHour);
        if (diffTime >= 0 && diffTime <= 30) {
            return "NORMAL";
        } else if (diffTime < 0) {
            return "EARLY";
        } else {
            return "OVER";
        }
    }

    private static int overTimeDurationCount(String timeOut, String lineTime) {
        int[] tempTimeIn = convertToLocaleTime(timeOut);
        String r[] = new String[2];
        String l[] = new String[2];
        r = timeOut.split(":");
        l = lineTime.split(":");
        //LocalDateTime fromDateTime = LocalDateTime.of(2015, 5, 5, tempTimeIn[0], tempTimeIn[1]);
        //LocalDateTime toDateTime = LocalDateTime.of(2015, 5, 5, 17, 00);
        LocalDateTime startWorkingHour = LocalDateTime.of(2000, 2, 2, Integer.parseInt(r[0]), Integer.parseInt(r[1]));
        LocalDateTime userStartWorking = LocalDateTime.of(2000, 2, 2, Integer.parseInt(l[0]), Integer.parseInt(l[1]));
        int diffTime = minuteCheck(userStartWorking, startWorkingHour);

        return diffTime;
    }
 
    private static void balanceCalculator(Double workinghour){
        int hour=0;
        int minute=0;
        double  per_hour = 36.25;
        double balance = 0;
        hour = workinghour.intValue()/60;
        minute = (int)(workinghour%60.0);
        balance = (hour*per_hour)+ Math.floor((per_hour/60)*minute);
        System.out.println("  Working Hour: "+hour+" hour "+minute+" minute(s)");
        System.out.println("  Salary: "+balance);
        
    }
}
