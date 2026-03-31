package com.mycompany.calendariopoo;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Calendario {
    
    List<Nota> lista_notas = new ArrayList<>();
    
    Scanner sc = new Scanner(System.in);
    
    int dia, mes, anno, opcion;
    String[] elementos;
    boolean salir = false;
    Fecha fechaInicio;
    
    public Calendario(Fecha fechaInicio){
        this.fechaInicio = fechaInicio;
    }
    
    public void runCalendario() throws Exception {
        while(!salir){
            mostrarMes(fechaInicio.getMes(), fechaInicio.getAnno());
        
            System.out.println("""
                               1-Ver mes
                               2-Ver notas por mes
                               3-Ver notas por dia
                               4-Ver todas las notas
                               5-Anadir una nota
                               6-Salir
                               Que quieres hacer?""");
            opcion = sc.nextInt();
            switch(opcion){
                case 1:
                    System.out.println("Introduce el mes que desea visualizar(mm/aaaa): ");
                    String visualizar_mes = sc.nextLine();
                    elementos = visualizar_mes.split("/");
                    mostrarMes(Integer.parseInt(elementos[0]), Integer.parseInt(elementos[1]));
                case 2:
                    System.out.print("Introduce el mes al que pertenecen las notas a visualizar(mm/aaaa): ");
                    String mes_notas = sc.nextLine();
                    elementos = mes_notas.split("/");
                    mes = Integer.parseInt(elementos[0]);
                    anno = Integer.parseInt(elementos[1]);
                    mostrarNotasMes(mes, anno);
                    System.out.println("1-Eliminar nota\n2-Modificar nota\n3-Salir\nQue quieres hacer?");
                    opcion = sc.nextInt();
                    switch(opcion){
                        case 1: 
                            for(int j=0; j<lista_notas.size(); j++){
                                if(mes==lista_notas.get(j).getMes() && anno==lista_notas.get(j).getAnno()){
                                    lista_notas.remove(j);
                                }
                            }
                            System.out.println("Nota eliminada correctamente");
                            break;
                        case 2: 
                            System.out.println("1-Nota simple\n2-Nota importante\n3-Cumpleanos\n4-Atras\nQue quieres hacer?");
                            opcion = sc.nextInt();
                            switch(opcion){
                                case 1:
                                    System.out.println("Que nota simple deseas modificar?");
                                    break;
                                case 2:
                                    System.out.println("1-Anadir nota importante\n2-Modificar nota importante\n3-Atras\nQue quieres hacer?");
                                    opcion = sc.nextInt();
                                    switch(opcion){
                                        case 1:
                                            break;
                                        case 2:
                                            break;
                                        case 3:
                                            break;
                                    }
                                    break;
                                case 3:
                                    System.out.println("1-Anadir cumpleanos\n2-Modificar cumpleanos\n3-Atras\nQue quieres hacer?");
                                    opcion = sc.nextInt();
                                    switch(opcion){
                                        case 1:
                                            System.out.println("");
                                            break;
                                        case 2:
                                            break;
                                        case 3:
                                            break;
                                    }
                                    break;
                                case 4:
                                    break;
                            }
                            break;
                        case 3:
                            break;
                    }
                    break;
                case 3:
                    System.out.println("Introduce el dia al que pertenecen las notas a visualizar(dd/mm/aaaa): ");
                    String dia_notas = sc.nextLine();
                    elementos = dia_notas.split("/");
                    mostrarNotas(Integer.parseInt(elementos[0]), Integer.parseInt(elementos[1]), Integer.parseInt(elementos[2]));
                    System.out.println("1-Eliminar nota\n2-Modificar nota\n3-Salir\nQue quieres hacer?");
                    opcion = sc.nextInt();
                    switch(opcion){
                        case 1: 
                            for(int j=0; j<lista_notas.size(); j++){
                                if(dia==lista_notas.get(j).getDia() && mes==lista_notas.get(j).getMes() && anno==lista_notas.get(j).getAnno()){
                                    lista_notas.remove(j);
                                }
                            }
                            System.out.println("Nota eliminada correctamente");
                            break;
                        case 2: 
                            System.out.println("1-Nota simple\n2-Nota importante\n3-Cumpleanos\n4-Atras\nQue quieres hacer?");
                            opcion = sc.nextInt();
                            break;
                        case 3:
                            break;
                    }
                    break;
                case 4:
                    mostrarTodasNotas();
                    break;
                case 5:
                    System.out.print("Introduce la fecha de la nota(dd/mm/aaaa): ");
                    String fecha_nota = sc.nextLine();
                    elementos = fecha_nota.split("/");
                    dia = Integer.parseInt(elementos[0]);
                    mes = Integer.parseInt(elementos[1]);
                    anno = Integer.parseInt(elementos[2]);
                    System.out.print("Introduce el nombre que llevara la nota: ");
                    String nombre = sc.nextLine();
                    System.out.println("Introduce el mensaje de la nota: ");
                    String mensaje = sc.nextLine();
                    Nota n = new Nota(dia, mes, anno, nombre, mensaje);
                    anadirNota(n);
                    System.out.println("La nota se ha anadido de forma correcta");
                    break;
                case 6:
                    salir = true;
                    break;
            }
        }
        System.out.println("Hasta la proxima!");
    }
    
    
    public void anadirNota(Nota nuevaNota) throws Exception{
        if(nuevaNota.getDia()>31)
            throw new Exception("Este dia del mes no existe");
        if(nuevaNota.getMes()>12)
            throw new Exception("Este mes no existe");
        if(nuevaNota.getDia()<1)
            throw new Exception("Este dia no existe");
        lista_notas.add(nuevaNota);
    }
    
    public void mostrarMes(int mes, int anno){
        System.out.print("-----------------------\n       ");
        int diasMes;
        switch(mes){
            case 1: System.out.println("Enero");
                diasMes = 31;
                break;
            case 2: System.out.println("Febrero");
                if(fechaInicio.esBisiesto()){
                    diasMes = 29;
                } else{
                    diasMes = 28;
                }
                break;
            case 3: System.out.println("Marzo");
                diasMes = 31;
                break;
            case 4: System.out.println("Abril");
                diasMes = 30;
                break;
            case 5: System.out.println("Mayo");
                diasMes = 31;
                break;
            case 6: System.out.println("Junio");
                diasMes = 30;
                break;
            case 7: System.out.println("Julio");
                diasMes = 31;
                break;
            case 8: System.out.println("Agosto");
                diasMes = 31;
                break;
            case 9: System.out.println("Septiembre");
                diasMes = 30;
                break;
            case 10: System.out.println("Octubre");
                diasMes = 31;
                break;
            case 11: System.out.println("Noviembre");
                diasMes = 30;
                break;
            default: System.out.println("Diciembre");
                diasMes = 31;
                break;
        }
        System.out.println("-----------------------");
        
        int z = zeller(mes, anno);
        int contador = 0;
        if(z==0)
            z = 7;
        for(int k=0; k<z-1; k++){
            System.out.print("|  ");
            contador ++;
        }
        for(int j=1; j<=diasMes; j++){
            if(j==1 || j==2 || j==3 || j==4 || j==5 || j==6 || j==7 || j==8 || j==9)
                System.out.print("| "+j);
            else
                System.out.print("|"+j);
            contador ++;
            if(contador==7){
                System.out.println("|");
                contador = 0;
            }
        }
        System.out.println();
    }
    
    public void mostrarTodasNotas(){
        int a = 1;
        System.out.println("Estas son todas las notas que hay guardadas.");
        for(int j=0; j<lista_notas.size(); j++){
            System.out.print(a+"-");
            System.out.println(lista_notas.get(j).toString());
            a++;
        }
    }
    
    public void mostrarNotasMes(int mes, int anno){
        boolean notas = false;
        int b = 1;
        System.out.print("------------------------\nAqui estan todas las notas del mes de ");
        switch(mes){
            case 1: System.out.println("Enero");
                break;
            case 2: System.out.println("Febrero");
                break;
            case 3: System.out.println("Marzo");
                break;
            case 4: System.out.println("Abril");
                break;
            case 5: System.out.println("Mayo");
                break;
            case 6: System.out.println("Junio");
                break;
            case 7: System.out.println("Julio");
                break;
            case 8: System.out.println("Agosto");
                break;
            case 9: System.out.println("Septiembre");
                break;
            case 10: System.out.println("Octubre");
                break;
            case 11: System.out.println("Noviembre");
                break;
            default: System.out.println("Diciembre");
                break;
            }
        System.out.println("------------------------");
        for(int i=0; i<lista_notas.size(); i++){
            if(mes==lista_notas.get(i).getMes() && anno==lista_notas.get(i).getAnno()){
                System.out.print(b+"-");
                System.out.println(lista_notas.get(i).toString());
                b ++;
                notas = true;
            }
        }
        if(!notas){
            System.out.println("No hay ninguna nota guardada para este mes");
        }
        System.out.println("------------------------");
    }
    
    public void mostrarNotas(int dia, int mes, int anno){
        boolean notas = false;
        int c = 1;
        System.out.println("Notas del dia "+dia+"/"+mes+"/"+anno);
        for(int i=0; i<lista_notas.size(); i++){
            if(dia==lista_notas.get(i).getDia() && mes==lista_notas.get(i).getMes() && anno==lista_notas.get(i).getAnno()){
                System.out.print(c+"-");
                System.out.println(lista_notas.get(i).toString());
                c ++;
                notas = true;
            }
        }
        if(!notas){
            System.out.println("No hay ninguna nota guardada para este dia");
        }
    }
    
    private int zeller(int mes, int anno){
        int a = (14-mes)/12;
        int y = anno - a;
        int m = mes + 12 * a - 2;
        int dia = 1, d;
        d = (dia + y + y / 4 - y / 100 + y / 400 + (31*m) / 12) % 7;
        return d;
    }
}
