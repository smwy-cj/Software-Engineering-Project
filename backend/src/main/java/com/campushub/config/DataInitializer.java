package com.campushub.config;

import com.campushub.entity.*;
import com.campushub.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserCertRepository userCertRepository;
    private final AdminRepository adminRepository;
    private final TreeHolePostRepository postRepository;
    private final PartnerReqRepository partnerReqRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, UserCertRepository userCertRepository,
                           AdminRepository adminRepository, TreeHolePostRepository postRepository,
                           PartnerReqRepository partnerReqRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userCertRepository = userCertRepository;
        this.adminRepository = adminRepository;
        this.postRepository = postRepository;
        this.partnerReqRepository = partnerReqRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // Create demo users
        User user1 = createUser("小明同学", "13800138001", "Abc12345");
        User user2 = createUser("小红同学", "13800138002", "Abc12345");
        User user3 = createUser("小刚同学", "13800138003", "Abc12345");
        User adminUser = createUser("管理员", "13800138000", "Admin123");

        // Create admin
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setAdminLevel("SUPER");
        admin.setPermissions("[\"user_manage\",\"content_manage\",\"review\",\"feedback\"]");
        adminRepository.save(admin);

        // Create certifications
        createCert(user1.getId(), "20240001", "张小明", "XX大学", "计算机科学与技术", "2024级", "male", 20);
        createCert(user2.getId(), "20240002", "李小红", "XX大学", "软件工程", "2024级", "female", 20);
        createCert(user3.getId(), "20240003", "王小刚", "XX大学", "计算机科学与技术", "2024级", "male", 21);

        // Create tree hole posts
        createPost(user1.getId(), "匿名小友327", "期末周太累了...图书馆位置都抢不到", "study", 42, 8);
        createPost(user2.getId(), "匿名小友451", "今天在食堂吃到了超好吃的麻辣烫！", "life", 28, 5);
        createPost(user3.getId(), "匿名小友189", "有人一起打篮球吗？下午四点半篮球场见", "fun", 35, 12);

        // Create partner requests
        PartnerReq req1 = new PartnerReq();
        req1.setUserId(user1.getId());
        req1.setType("study");
        req1.setDescription("寻找一起备考六级的学习搭子，每天下午图书馆自习");
        req1.setConditions("{\"grade\":\"2024级\",\"major\":\"计算机科学与技术\",\"gender\":\"any\"}");
        req1.setValidDays(5);
        req1.setMaxMembers(3);
        req1.setVisibility("sameSchool");
        req1.setStatus("PUBLISHED");
        req1.setExpireTime(LocalDateTime.now().plusDays(5));
        partnerReqRepository.save(req1);

        PartnerReq req2 = new PartnerReq();
        req2.setUserId(user2.getId());
        req2.setType("sport");
        req2.setDescription("寻找跑步搭子，每天晚上操场5公里");
        req2.setConditions("{\"gender\":\"female\"}");
        req2.setValidDays(3);
        req2.setMaxMembers(2);
        req2.setVisibility("sameSchool");
        req2.setStatus("PUBLISHED");
        req2.setExpireTime(LocalDateTime.now().plusDays(3));
        partnerReqRepository.save(req2);
    }

    private User createUser(String username, String phone, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAccountStatus("NORMAL");
        return userRepository.save(user);
    }

    private void createCert(Long userId, String studentId, String realName, String university,
                            String major, String grade, String gender, int age) {
        UserCert cert = new UserCert();
        cert.setUserId(userId);
        cert.setStudentId(studentId);
        cert.setRealName(realName);
        cert.setIdCard("32000020000101000" + userId);
        cert.setUniversity(university);
        cert.setMajor(major);
        cert.setGrade(grade);
        cert.setGender(gender);
        cert.setAge(age);
        cert.setInterests("[\"读书\",\"运动\",\"编程\"]");
        cert.setCertStatus("CERTIFIED");
        userCertRepository.save(cert);
    }

    private void createPost(Long userId, String anonName, String content, String category, int likes, int comments) {
        TreeHolePost post = new TreeHolePost();
        post.setUserId(userId);
        post.setAnonymousName(anonName);
        post.setContent(content);
        post.setCategory(category);
        post.setStatus("PUBLISHED");
        post.setLikeCount(likes);
        post.setCommentCount(comments);
        postRepository.save(post);
    }
}
