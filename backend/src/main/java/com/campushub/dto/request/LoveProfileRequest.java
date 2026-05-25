package com.campushub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoveProfileRequest {
    @NotBlank private String gender;
    @Min(16) @Max(60) private int age;
    private Integer height;
    private Integer weight;
    private String constellation;
    private String interests;
    @NotBlank private String matePreference;
    @NotBlank @Size(min = 10, max = 100) private String declaration;
    private String visibility = "all";

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public String getConstellation() { return constellation; }
    public void setConstellation(String constellation) { this.constellation = constellation; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getMatePreference() { return matePreference; }
    public void setMatePreference(String matePreference) { this.matePreference = matePreference; }
    public String getDeclaration() { return declaration; }
    public void setDeclaration(String declaration) { this.declaration = declaration; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
}
