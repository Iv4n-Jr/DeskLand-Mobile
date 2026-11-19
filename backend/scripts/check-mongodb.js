// Verifica o MongoDB Atlas (ou outro MongoDB configurado em MONGO_URI).
// Executar dentro do container: docker compose exec -T api node scripts/check-mongodb.js
const mongoose = require('mongoose');

async function verificar() {
  if (!process.env.MONGO_URI) {
    throw new Error('Variavel MONGO_URI nao configurada');
  }

  try {
    await mongoose.connect(process.env.MONGO_URI, {
      serverSelectionTimeoutMS: 15000,
    });
    const resultado = await mongoose.connection.db.admin().ping();
    if (resultado.ok !== 1) throw new Error('O banco nao respondeu ao ping');
    console.log('Conexao MongoDB confirmada: ping OK');
  } finally {
    await mongoose.disconnect();
  }
}

verificar().catch((erro) => {
  console.error('Falha na verificacao do MongoDB:', erro.message);
  process.exitCode = 1;
});
