package com.codegroup.portfolio.entity;

import com.codegroup.portfolio.enums.StatusProjeto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projeto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "previsao_termino", nullable = false)
    private LocalDate previsaoTermino;

    @Column(name = "data_real_termino")
    private LocalDate dataRealTermino;

    @Column(name = "orcamento_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal orcamentoTotal;

    @Column(length = 2000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerente_id")
    private Membro gerenteResponsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusProjeto status = StatusProjeto.EM_ANALISE;

    @ManyToMany
    @JoinTable(
            name = "projeto_membro",
            joinColumns = @JoinColumn(name = "projeto_id"),
            inverseJoinColumns = @JoinColumn(name = "membro_id")
    )
    @Builder.Default
    private List<Membro> membros = new ArrayList<>();
}
