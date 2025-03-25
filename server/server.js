const express = require('express');
const { Pool } = require('pg');
const app = express();
const port = 3000;

// Configuração do PostgreSQL
const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'marcca-ponto-bd',
    password: 'postgres',
    port: 5432,
});

// Middleware para parsear JSON
app.use(express.json());

// Rota para salvar o ponto
app.post('/salvar-ponto', async (req, res) => {
    const { usuario, latitude, longitude, horario } = req.body;

    try {
        const query = `
            INSERT INTO pontos (usuario, latitude, longitude, horario)
            VALUES ($1, $2, $3, $4)
            RETURNING *;
        `;
        const values = [usuario, latitude, longitude, horario];

        const result = await pool.query(query, values);
        res.status(201).json(result.rows[0]);
    } catch (error) {
        console.error('Erro ao salvar ponto:', error);
        res.status(500).json({ error: 'Erro ao salvar ponto' });
    }
});

// Iniciar o servidor
app.listen(port, () => {
    console.log(`Servidor rodando na porta ${port}`);
});


// Rota para buscar pontos do dia atual
const moment = require('moment-timezone');

app.get('/pontos-do-dia/:usuario', async (req, res) => {
    const { usuario } = req.params;

    // Definir o intervalo do dia no fuso horário local
    const timeZone = 'America/Sao_Paulo'; // Ajuste para o fuso horário correto
    const inicioDoDia = moment().tz(timeZone).startOf('day').toISOString(true);
    const fimDoDia = moment().tz(timeZone).endOf('day').toISOString(true);

    try {
        const query = `
            SELECT usuario, latitude, longitude, horario
            FROM pontos
            WHERE horario >= $1 AND horario < $2 AND usuario = $3
            ORDER BY horario DESC;
        `;
        const values = [inicioDoDia, fimDoDia, usuario];

        console.log("Buscando pontos do dia:", inicioDoDia, "até", fimDoDia);
        console.log("Query:", query);
        console.log("Valores:", values);

        const result = await pool.query(query, values);
        console.log("Pontos encontrados:", result.rows);

        res.status(200).json(result.rows);
    } catch (error) {
        console.error('Erro ao buscar pontos:', error);
        res.status(500).json({ error: 'Erro ao buscar pontos' });
    }
});


// Rota para contar pontos do usuário no dia atual
app.get('/contar-pontos-do-dia/:usuario', async (req, res) => {
    const { usuario } = req.params;
    const dataAtual = new Date().toLocaleDateString('pt-BR').split('/').reverse().join('-');

    try {
        const query = `
            SELECT COUNT(*) as total
            FROM pontos
            WHERE usuario = $1 AND DATE(horario) = $2;
        `;
        const values = [usuario, dataAtual];

        const result = await pool.query(query, values);
        const totalPontos = result.rows[0].total;

        console.log("Contando pontos do usuário:", usuario, "na data:", dataAtual, "Total encontrado: ", totalPontos);

        res.status(200).json({ total: totalPontos });
    } catch (error) {
        console.error('Erro ao contar pontos:', error);
        res.status(500).json({ error: 'Erro ao contar pontos' });
    }
});



app.post('/salvar-configuracao', async (req, res) => {
    const { usuario, entrada1, saida1, entrada2, saida2 } = req.body;

    try {
        const query = `
            INSERT INTO configuracao_horarios (usuario, entrada1, saida1, entrada2, saida2)
            VALUES ($1, $2, $3, $4, $5)
            ON CONFLICT (usuario) DO UPDATE
            SET entrada1 = $2, saida1 = $3, entrada2 = $4, saida2 = $5;
        `;
        const values = [usuario, entrada1, saida1, entrada2, saida2];

        await pool.query(query, values);
        res.status(200).json({ message: 'Configuração salva com sucesso!' });
    } catch (error) {
        console.error('Erro ao salvar configuração:', error);
        res.status(500).json({ error: 'Erro ao salvar configuração' });
    }
});


app.get('/buscar-pontos-por-horario', async (req, res) => {
    const { horario } = req.query;

    try {
        const query = `
            SELECT * FROM pontos
            WHERE TO_CHAR(horario, 'HH24:MI') = $1;
        `;
        const values = [horario];

        const result = await pool.query(query, values);
        res.status(200).json(result.rows);
    } catch (error) {
        console.error('Erro ao buscar pontos:', error);
        res.status(500).json({ error: 'Erro ao buscar pontos' });
    }
});

app.get('/buscar-horarios-configurados', async (req, res) => {
    const { usuario } = req.query;

    try {
        const query = `
            SELECT entrada1, saida1, entrada2, saida2
            FROM configuracao_horarios
            WHERE usuario = $1;
        `;
        const values = [usuario];

        const result = await pool.query(query, values);
        if (result.rows.length > 0) {
            res.status(200).json(result.rows[0]);
        } else {
            res.status(404).json({ error: 'Configuração não encontrada' });
        }
    } catch (error) {
        console.error('Erro ao buscar horários configurados:', error);
        res.status(500).json({ error: 'Erro ao buscar horários configurados' });
    }
});
