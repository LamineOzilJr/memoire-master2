package sn.groupeisi.leaveworkflow.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import sn.groupeisi.leaveworkflow.dto.UserDto;
import sn.groupeisi.leaveworkflow.dto.ManagerStatsDto;
import sn.groupeisi.leaveworkflow.enums.Role;

import java.util.List;

public interface UserService extends UserDetailsService {
    List<UserDto> getAllUsers();
    UserDto getUserById(Long id);
    UserDto createUser(UserDto userDto);
    UserDto updateUser(Long id, UserDto userDto);
    void deleteUser(Long id);
    UserDto mapToDto(sn.groupeisi.leaveworkflow.model.User user);

    // Used in @PreAuthorize SpEL to check ownership
    boolean isCurrentUser(Long id, Authentication authentication);

    // New method
    ManagerStatsDto getManagerStats(Long managerId);

    // Get max matricule sequence for a given enterprise and role
    Long getMaxMatriculeSequence(Long entrepriseId, Role role);
}
