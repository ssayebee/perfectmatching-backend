package com.matching.domain.dto;

import com.matching.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {

    private Long userIdx;

    @NotBlank
    @Length(max = 30, min = 1)
    private String nickname;

    @NotBlank
    @Length(max = 500)
    private String summary;

    private String profileImage;

    @NotBlank
    @Email
    @Length(max = 30, min = 3)
    private String email;

    @NotBlank
    @Length(max = 100)
    private String profileImg;

    @NotBlank
    private Integer investTime;

    @Length(max = 100)
    private String socialUrl;

    public ProfileDTO(User user) {
        this.userIdx = user.getIdx();
        this.nickname = user.getNick();
        this.summary = user.getDescription();
        this.profileImage = user.getProfileImg();
        this.email = user.getEmail();
        this.profileImg = user.getProfileImg();
        this.investTime = user.getInvestTime();
        this.socialUrl = user.getSocialUrl();
    }

}
