package mfec;
	


public class Employee {
	private String name="";
	private double workingHour;
	public Employee(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	
	public void increaseworkingHour(double workingHour){
		this.workingHour += workingHour;
	}
        public double getWorkingHour(){
           return workingHour;
            
        }
	



}
