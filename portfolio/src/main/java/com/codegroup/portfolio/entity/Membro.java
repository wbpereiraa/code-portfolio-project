package com.codegroup.portfolio.entity;

import com.codegroup.portfolio.enums.AtribuicaoMembro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representação local (cache) do membro retornado pela API externa mockada.
 * O cadastro "oficial" de membros é feito via API externa; aqui apenas
 * persistimos os dados necessários para relacionamento com Projeto.
 */
@Entity
@Table(name = "membro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membro {

    @Id
    private Long id; // mesmo id retornado pela API externa mockada

    private String nome;

    @Enumerated(EnumType.STRING)
    private AtribuicaoMembro atribuicao;
}
