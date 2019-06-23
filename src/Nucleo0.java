public class Nucleo0 extends Thread {
    private int registro[];
    private int quantumTotal;
    private int quantumHililloActual;
    private CacheDatosC cacheDatosLocal;
    private CacheDatosD cacheDatosNucleo1;
    private int PC;
    private int RL;
    private int idHililloActual;
    private Planificador planificador;
    private Pcb pcb;

    public Nucleo0(cacheDatosC local, chacheDatosD cacheDatosNucleo1, int quantum, Planificador planificador){
        this.cacheDAtosLocal = local;
        this.cacheDatosNucleo1 = cacheDatosNucleo1;
        registro = new int[32];
        this.planificador = planificador;
    }

    public void copiarPcbAContextoActual(Pcb pcb){
        this.registro = pcb.getRegistro();
        this.idHililloActual = pcb.getId();
        this.PC = pcb.getPc();
        this.RL = -1;
    }

    // Guarda el contexto del hilo actual y carga el contexto del siguiente hilo
    public void siguienteHilillo(bool termiando, Pcb pcb){
        if(!terminado){
            pcb.setEstado('R');
            pcb.setRegistro(this.registro);
            pcb.setPc(this.PC);
            planificador.agregarProcesosRestantes(pcb);
        }
        else{
            pcb.setEstado('F');
            pcb.setRegistro(this.registro);
            pcb.setPc(this.PC);
            planificador.agregarProcesosTerminados(pcb);
        }
        this.pcb = planificador.usarProcesosRestantes();
        copiarPcbAContextoActual(pcb);
    }

    public int Alu(int operacion, int operando1, int operando2){
        int resultado = -1;
        switch(operacion){
            case 1:
                resultado = operando1 + operando2;
                break;
            case 2:
                resultado = operando1 - operando2;
                break;
            case 3:
                resultado = operando1 * operando2;
                break:
            case 4:
                resultado = operando1 / operando2;
                break;
        }
        return resultado;
    }

    public void decodificador(int[] instruccion){
        switch(instruccion[0]){
            case 19:
                registro[instruccion[1]] = Alu(1, registro[instruccion[2]], instruccion[3]);
                break;
            case 71:
                registro[instruccion[1]] = Alu(1, registro[instruccion[2]], instruccion[3]);
                break;
            case 83:
                registro[instruccion[1]] = Alu(2, registro[instruccion[2]], instruccion[3]);
                break;
            case 72:
                registro[instruccion[1]] = Alu(3, registro[instruccion[2]], instruccion[3]);
                break;
            case 56:
                registro[instruccion[1]] = Alu(4, registro[instruccion[2]], instruccion[3]);
                break;
            case 5:
                break;
            case 37:
                break;
            case 99:
                int resultado = Alu(2, registro[instruccion[1]], instruccion[2]);
                if(resultado == 0){
                    PC += (instruccion[3] * 4);
                }
                break;
            case 100:
                int resultado = Alu(2, registro[instruccion[1]], instruccion[2]);
                if(resultado != 0){
                    PC += (instruccion[3] * 4);
                }
                break;
            case 51:
                break;
            case 52:
                break;
            case 111:
                registro[instruccion[1]] = PC;
                PC = PC + instruccion[3];
                break;
            case 103:
                registro[instruccion[1]] = PC;
                PC = instruccion[2] + instruccion[3];
                break;
            case 999:
                // Se guarda pcb en lista de procesos terminados
                
                break;
        }
    }

    public void run(){
        // Este es la función que se llame con el nucleo0.start()
    }
}
