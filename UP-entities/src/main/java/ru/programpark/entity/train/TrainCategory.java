package ru.programpark.entity.train;

public class TrainCategory {

	private Long catId;
	private Integer priority;
	
	public TrainCategory(Long catId, Integer priority) {
		this.catId = catId;
		this.priority = priority;
	}
	
	public TrainCategory(Long catId) {
		this.catId = catId;
	}
	
	public TrainCategory() {
		
	}

	public Long getCatId() {
		return catId;
	}

	public void setCatId(Long catId) {
		this.catId = catId;
	}

	public Integer getPriority() {
		if (priority != null){
			return priority;
		} else {
			return 100;
		}
			
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
}
