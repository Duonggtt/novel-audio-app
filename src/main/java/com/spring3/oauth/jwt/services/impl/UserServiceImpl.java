package com.spring3.oauth.jwt.services.impl;

import com.spring3.oauth.jwt.entity.Genre;
import com.spring3.oauth.jwt.entity.Tier;
import com.spring3.oauth.jwt.entity.enums.UserStatusEnum;
import com.spring3.oauth.jwt.exception.NotFoundException;
import com.spring3.oauth.jwt.models.dtos.UserResponseDTO;
import com.spring3.oauth.jwt.models.request.ForgotPassRequest;
import com.spring3.oauth.jwt.models.request.GenresSelectedRequest;
import com.spring3.oauth.jwt.models.request.UpdateUserRequest;
import com.spring3.oauth.jwt.models.request.UserRequest;
import com.spring3.oauth.jwt.models.response.UserResponse;
import com.spring3.oauth.jwt.entity.User;
import com.spring3.oauth.jwt.repositories.GenreRepository;
import com.spring3.oauth.jwt.repositories.TierRepository;
import com.spring3.oauth.jwt.repositories.UserRepository;
import com.spring3.oauth.jwt.services.EmailService;
import com.spring3.oauth.jwt.services.UserService;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    ModelMapper modelMapper = new ModelMapper();
    @Autowired
    private TierRepository tierRepository;
    @Autowired
    private GenreRepository genreRepository;


    // Hàm xử lý quên mật khẩu
    @Override
    public void forgotPass(ForgotPassRequest request) {
        User user = userRepository.findByEmail(request.getEmail());

        if (user == null) {
            throw new RuntimeException("User with email: " + request.getEmail() + " is not found..!!");
        }

        try {
            // Tạo mã OTP ngẫu nhiên
            String otpCode = generateOTP();

            // Lưu OTP vào database
            user.setOtpCode(otpCode);  // Đảm bảo entity `User` có trường `otpCode`
            userRepository.save(user);

            // Gửi mã OTP qua email
            emailService.sendOtpToEmail(request.getEmail(), otpCode);
        } catch (Exception e) {
            // Log lỗi để xác định nguyên nhân
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }


    @Override
    public UserResponse resetPassword(String email, String newPassword) {
        // Tìm người dùng qua email
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User with email: " + email + " not found..!!");
        }

        // Mã hóa mật khẩu mới

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(newPassword);

        // Cập nhật mật khẩu
        user.setPassword(encodedPassword);
        user.setOtpCode(null);
        userRepository.save(user);
        return modelMapper.map(user, UserResponse.class);
    }

    // Hàm tạo mã OTP
    private String generateOTP() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));  // Tạo mã OTP 6 chữ số
    }

    // Hàm xác nhận OTP
    @Override
    public String verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User with email: " + email + " is not found..!!");
        }

        // Kiểm tra xem OTP có khớp không
        if (!user.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("OTP is invalid");
        }

        // Nếu OTP đúng, trả về thông báo thành công
        return "OTP is valid";
    }

    @Override
    public void updateReadCountChapter(long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        int newCount = user.getChapterReadCount() + 1;
        user.setChapterReadCount(newCount);
        List<Tier> tierList = tierRepository.findAll();
        for (Tier value : tierList) {
            if (newCount == value.getReadCountRequired()) {
                Tier tier = tierRepository.findByReadCountRequired(newCount);
                user.setTier(tier);
                break;
            }
        }
        userRepository.save(user);
    }

    @Override
    public String confirmPaymentStatus(String username) {
        User user = userRepository.findByUsername(username);
        if(user == null) {
            throw new NotFoundException("Not found");
        }
        user.setStatus(UserStatusEnum.ACTIVE);
        userRepository.save(user);
        return "ACTIVE";
    }

    @Override
    public boolean isAdmin(String username) {
        User user = userRepository.findByUsername(username);
        String role = null;
        if(user.getRoles().isEmpty()) {
            return false;
        }else {
            role = user.getRoles().stream().findFirst().get().getName();
        }
        if(role.equals("ROLE_ADMIN")) {
            return true;
        }
        return false;
    }

    @Override
    public UserResponse saveUser(UserRequest userRequest) {
        if(userRequest.getUsername() == null){
            throw new RuntimeException("Parameter username is not found in request..!!");
        } else if(userRequest.getPassword() == null){
            throw new RuntimeException("Parameter password is not found in request..!!");
        }else if(userRequest.getEmail() == null){
            throw new RuntimeException("Parameter email is not found in request..!!");
        }

        List<User> userListExistUsername = userRepository.findAllByUsername(userRequest.getUsername());

        List<User> userListExistEmail = userRepository.findAllByEmail(userRequest.getEmail());

        if(!userListExistUsername.isEmpty()) {
            throw new RuntimeException("Username is already exist..!!");
        }
        if(!userListExistEmail.isEmpty()) {
            throw new RuntimeException("Email is already exist..!!");
        }

//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        UserDetails userDetail = (UserDetails) authentication.getPrincipal();
//        String usernameFromAccessToken = userDetail.getUsername();
//
//        User currentUser = userRepository.findByUsername(usernameFromAccessToken);

        User savedUser = null;

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = userRequest.getPassword();
        String encodedPassword = encoder.encode(rawPassword);

        User user = modelMapper.map(userRequest, User.class);
        user.setPassword(encodedPassword);
        user.setRoles(null);
        user.setStatus(UserStatusEnum.INACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDob(null);
        user.setChapterReadCount(0);
        user.setSelectedGenres(null);
        if(userRequest.getId() != null){
            User oldUser = userRepository.findFirstById(userRequest.getId());
            if(oldUser != null){
                oldUser.setId(user.getId());
                oldUser.setPassword(user.getPassword());
                oldUser.setUsername(user.getUsername());
                oldUser.setEmail(user.getEmail());
                oldUser.setRoles(null);
                oldUser.setStatus(UserStatusEnum.INACTIVE);
                oldUser.setImagePath(null);
                oldUser.setCreatedAt(LocalDateTime.now());
                oldUser.setUpdatedAt(LocalDateTime.now());
                oldUser.setDob(null);
                oldUser.setChapterReadCount(user.getChapterReadCount());
                oldUser.setSelectedGenres(null);
                savedUser = userRepository.save(oldUser);
                userRepository.refresh(savedUser);
            } else {
                throw new RuntimeException("Can't find record with identifier: " + userRequest.getId());
            }
        } else {
//            user.setCreatedBy(currentUser);
            savedUser = userRepository.save(user);
        }
        userRepository.refresh(savedUser);
        UserResponse userResponse = modelMapper.map(savedUser, UserResponse.class);
        return userResponse;
    }

    @Override
    public UserResponse getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetail = (UserDetails) authentication.getPrincipal();
        String usernameFromAccessToken = userDetail.getUsername();
        User user = userRepository.findByUsername(usernameFromAccessToken);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        return userResponse;
    }

    @Override
    public UserResponseDTO getProfile(String username) {
        User user = userRepository.findByUsername(username);
        if(user == null){
            throw new RuntimeException("User with username: " + username + " is not found..!!");
        }
        return convertToDTO(user);
    }

    @Override
    public UserResponseDTO updateProfile(UpdateUserRequest request, String username) {
        User user = userRepository.findByUsername(username);
        if(user == null){
            throw new RuntimeException("User with username: " + username + " is not found..!!");
        }
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setDob(request.getDob());
        userRepository.save(user);
        return convertToDTO(user);
    }

    @Override
    public UserResponseDTO updateSelectedGenres(Long userId, GenresSelectedRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        List<Genre> selectedGenres = genreRepository.findAllById(request.getSelectedGenreIds());
        if(selectedGenres.isEmpty()) {
            throw new NullPointerException("Genre ids is null!");
        }
        user.setSelectedGenres(selectedGenres);
        userRepository.save(user);
        return convertToDTO(user);
    }

    UserResponseDTO convertToDTO(User user) {
        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(user.getId());
        userResponseDTO.setFullName(user.getFullName());
        userResponseDTO.setUsername(user.getUsername());
        userResponseDTO.setEmail(user.getEmail());
        userResponseDTO.setChapterReadCount(user.getChapterReadCount());
        userResponseDTO.setAccountStatus(String.valueOf(user.getStatus()));
        if(user.getTier() == null){
            userResponseDTO.setTierName("No tier");
        }else {
            userResponseDTO.setTierName(user.getTier().getName());
        }
        userResponseDTO.setImagePath(user.getImagePath());
        userResponseDTO.setCreatedAt(user.getCreatedAt());
        userResponseDTO.setUpdatedAt(user.getUpdatedAt());
        if(user.getSelectedGenres().isEmpty()) {
            userResponseDTO.setSelectedGenreIds(null);
        }else{
            userResponseDTO.setSelectedGenreIds(user.getSelectedGenres()
                .stream()
                .map(Genre::getId)
                .toList()
            );
        }
        return userResponseDTO;
    }

    @Override
    public List<UserResponse> getAllUser() {
        List<User> users = (List<User>) userRepository.findAll();
        Type setOfDTOsType = new TypeToken<List<UserResponse>>(){}.getType();
        List<UserResponse> userResponses = modelMapper.map(users, setOfDTOsType);
        return userResponses;
    }



}
