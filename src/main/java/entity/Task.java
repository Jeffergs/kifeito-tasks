package entity;

import com.kifeito.tasks.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "T_KFT_TASK")
public class Task {
    @id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 100)
    private String description;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime createAt;

    private LocalDateTime scheduletAt;

    @Column(nullable = false)
    private LocalDateTime updateAt;

    @Enumerated(EnumType.STRING)
    private Status status;









}
