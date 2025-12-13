package sn.groupeisi.leaveworkflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sn.groupeisi.leaveworkflow.dto.UserDto;
import sn.groupeisi.leaveworkflow.model.Departement;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.DepartementRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartementRepository departementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDto(user);
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        Departement departement = null;
        if (userDto.getDepartementId() != null) {
            departement = departementRepository.findById(userDto.getDepartementId()).orElse(null);
        }
        User manager = null;
        if (userDto.getManagerId() != null) {
            manager = userRepository.findById(userDto.getManagerId()).orElse(null);
        }

        User user = User.builder()
                .prenom(userDto.getPrenom())
                .nom(userDto.getNom())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode("changeme")) // default password, should be reset by user
                .telephone(userDto.getTelephone())
                .adresse(userDto.getAdresse())
                .role(userDto.getRole())
                .poste(userDto.getPoste())
                .matricule(userDto.getMatricule())
                .departement(departement)
                .manager(manager)
                .active(userDto.getActive() != null ? userDto.getActive() : true)
                .build();

        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        if (userDto.getPrenom() != null) user.setPrenom(userDto.getPrenom());
        if (userDto.getNom() != null) user.setNom(userDto.getNom());
        if (userDto.getEmail() != null) user.setEmail(userDto.getEmail());
        if (userDto.getTelephone() != null) user.setTelephone(userDto.getTelephone());
        if (userDto.getAdresse() != null) user.setAdresse(userDto.getAdresse());
        if (userDto.getRole() != null) user.setRole(userDto.getRole());
        if (userDto.getPoste() != null) user.setPoste(userDto.getPoste());
        if (userDto.getMatricule() != null) user.setMatricule(userDto.getMatricule());
        if (userDto.getDepartementId() != null) {
            Departement dep = departementRepository.findById(userDto.getDepartementId()).orElse(null);
            user.setDepartement(dep);
        }
        if (userDto.getManagerId() != null) {
            User mgr = userRepository.findById(userDto.getManagerId()).orElse(null);
            user.setManager(mgr);
        }
        if (userDto.getActive() != null) user.setActive(userDto.getActive());

        User updated = userRepository.save(user);
        return mapToDto(updated);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDto mapToDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .prenom(user.getPrenom())
                .nom(user.getNom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .adresse(user.getAdresse())
                .role(user.getRole())
                .poste(user.getPoste())
                .matricule(user.getMatricule())
                .departementId(user.getDepartement() != null ? user.getDepartement().getId() : null)
                .departementLibelle(user.getDepartement() != null ? user.getDepartement().getLibelle() : null)
                .managerId(user.getManager() != null ? user.getManager().getId() : null)
                .managerFullName(user.getManager() != null ? user.getManager().getFullName() : null)
                .active(user.getActive())
                .build();
    }

    @Override
    public boolean isCurrentUser(Long id, org.springframework.security.core.Authentication authentication) {
        if (authentication == null) return false;
        String email = authentication.getName();
        return userRepository.findById(id).map(u -> u.getEmail().equals(email)).orElse(false);
    }
}

