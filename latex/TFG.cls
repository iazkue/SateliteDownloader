\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{TFG}[2023-09-12 TFG document template]



\DeclareOption*{\PassOptionsToClass{\CurrentOption}{book}}
\ProcessOptions\relax
\LoadClass[a4paper,11pt]{book}




% Define the required packages
\RequirePackage[utf8]{inputenc}
\RequirePackage[T1]{fontenc}
\RequirePackage{hyperref}
\RequirePackage{graphicx,amsfonts,psfrag,fancyhdr,layout}
\RequirePackage{xcolor}
\RequirePackage{colortbl}
\RequirePackage[width=15cm, outer=2.5cm]{geometry}
\RequirePackage[Bjornstrup]{fncychap}
\RequirePackage[all]{hypcap}
\RequirePackage{authorindex}
\RequirePackage{float}
\RequirePackage{subcaption}
\RequirePackage{listings}
\RequirePackage{upquote}
\RequirePackage{amsthm,thmtools}
\RequirePackage[final]{pdfpages}
\RequirePackage{imakeidx}
\RequirePackage[titletoc]{appendix}


\newtheorem{theorem}{Theorem}
\newtheorem{definition}{Definition}
\newtheorem{lemma}{Lemma}


\definecolor{refcolor1}{rgb}{0.00,0.00,0.63}
\definecolor{refcolor2}{rgb}{0.00,0.00,1.00}

\hypersetup{colorlinks=true,
linkcolor={refcolor1},
filecolor={refcolor1},
urlcolor={refcolor2},
citecolor={refcolor2}}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Listings settings %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% General Setting of listings 
\lstset{
    %style=Python,
    tabsize=4,
    aboveskip=1em,
    breaklines=true,
    rulecolor=\color{black},
    %abovecaptionskip=-6pt,
    captionpos=b,
    escapeinside={\%*}{*)},
    frame=single,
    numbers=left,
    numbersep=8pt,
    numberstyle=\tiny,
   	literate={á}{{\'a}}1
   		{é}{{\'e}}1
   		{í}{{\'{\i}}}1
   		{ó}{{\'o}}1
   		{ú}{{\'u}}1
   		{Á}{{\'A}}1
   		{É}{{\'E}}1
   		{Í}{{\'I}}1
   		{Ó}{{\'O}}1
   		{Ú}{{\'U}}1
   		{ü}{{\"u}}1
   		{Ü}{{\"U}}1
   		{ñ}{{\~n}}1
   		{Ñ}{{\~N}}1
   		{¿}{{?`}}1
   		{¡}{{!`}}1
}


% PHP style

\definecolor{dkgreen}{rgb}{0,.6,0}
\definecolor{dkblue}{rgb}{0,0,.6}
\definecolor{dkyellow}{cmyk}{0,0,.8,.3}

\lstdefinestyle{php}{
  language        = php,
  keywordstyle    = \color{dkblue},
  stringstyle     = \color{red},
  identifierstyle = \color{dkgreen},
  commentstyle    = \color{gray},
  emph            =[1]{php},
  emphstyle       =[1]\color{black},
  emph            =[2]{if,and,or,else},
  emphstyle       =[2]\color{dkyellow}}

% Python style
\definecolor{darkred}{rgb}{0.6,0.0,0.0}
\definecolor{darkgreen}{rgb}{0,0.50,0}
\definecolor{lightblue}{rgb}{0.0,0.42,0.91}
\definecolor{orange}{rgb}{0.99,0.48,0.13}
\definecolor{grass}{rgb}{0.18,0.80,0.18}
\definecolor{pink}{rgb}{0.97,0.15,0.45}

\lstdefinestyle{Python}{
    language=Python,
morekeywords=[1]{,as,assert,nonlocal,with,yield,self,True,False,None,} % Python builtin
 morekeywords=[2]{,__init__,__add__,__mul__,__div__,__sub__,__call__,__getitem__,__setitem__,__eq__,__ne__,__nonzero__,__rmul__,__radd__,__repr__,__str__,__get__,__truediv__,__pow__,__name__,__future__,__all__,}, % magic methods
 morekeywords=[3]{,object,type,isinstance,copy,deepcopy,zip,enumerate,reversed,list,set,len,dict,tuple,range,xrange,append,execfile,real,imag,reduce,str,repr,}, % common functions
 morekeywords=[4]{,Exception,NameError,IndexError,SyntaxError,TypeError,ValueError,OverflowError,ZeroDivisionError,}, % errors
 morekeywords=[5]{,ode,fsolve,sqrt,exp,sin,cos,arctan,arctan2,arccos,pi, array,norm,solve,dot,arange,isscalar,max,sum,flatten,shape,reshape,find,any,all,abs,plot,linspace,legend,quad,polyval,polyfit,hstack,concatenate,vstack,column_stack,empty,zeros,ones,rand,vander,grid,pcolor,eig,eigs,eigvals,svd,qr,tan,det,logspace,roll,min,mean,cumsum,cumprod,diff,vectorize,lstsq,cla,eye,xlabel,ylabel,squeeze,}, % numpy / math
    morestring=*[d]{"},
    basicstyle=\ttfamily,
    backgroundcolor=\color{white},
    commentstyle=\color{darkgreen}\slshape,
    keywordstyle=\color{blue}\bfseries\itshape,
    keywordstyle=[2]\color{blue}\bfseries,
    keywordstyle=[3]\color{grass},
    keywordstyle=[4]\color{red},
    keywordstyle=[5]\color{orange},
    stringstyle=\color{darkred},
    emphstyle=\color{pink}\underbar,
} 


% R style
\lstdefinestyle{R}{ %
  language=R,                     % the language of the code
  title=\lstname,                 % show the filename of files included with \lstinputlisting;
                                  % also try caption instead of title
  keywordstyle=\color{blue},      % keyword style
  commentstyle=\color{darkgreen},   % comment style
  stringstyle=\color{darkred},      % string literal style
  escapeinside={\%*}{*)},         % if you want to add a comment within your code
  morekeywords={*,...}            % if you want to add more keywords to the set
} 


% Java style
\definecolor{javablue}{rgb}{0.13,0.13,1}
\definecolor{javagreen}{rgb}{0,0.5,0}
\definecolor{javared}{rgb}{0.9,0,0}
\definecolor{javagrey}{rgb}{0.46,0.45,0.48}

\lstdefinestyle{Java}{
    language=java,
	commentstyle=\color{javagreen},
	keywordstyle=\color{javablue},
	stringstyle=\color{javared},
	moredelim=[il][\textcolor{javagrey}]{\$\$},
	moredelim=[is][\textcolor{javagrey}]{\%\%}{\%\%}
}


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Code for creating empty pages                 %
% No headers on empty pages before new chapter  %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\makeatletter
\makeindex
\def\cleardoublepage{\clearpage\if@twoside \ifodd\c@page\else
    \hbox{}
    \thispagestyle{plain}
    \newpage
    \if@twocolumn\hbox{}\newpage\fi\fi\fi}
\makeatother \clearpage{\pagestyle{plain}\cleardoublepage}


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Code for creating chapter 1st page            %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\pagestyle{fancy}
\renewcommand{\chaptermark}[1]{\markboth{#1}{}}
\renewcommand{\sectionmark}[1]{\markright{\thesection\ #1}}
\fancyhf{}

\newcommand{\changefont}{%
    \fontsize{11.5}{11}\selectfont
}

\fancyhead[LE,RO]{\changefont\bfseries\thepage}
\fancyhead[LO]{\changefont\bfseries\rightmark}
\fancyhead[RE]{\changefont\bfseries\leftmark}
\renewcommand{\headrulewidth}{0.5pt}
\renewcommand{\footrulewidth}{0pt}
\addtolength{\headheight}{0.5pt}
\setlength{\footskip}{0in}
\renewcommand{\footruleskip}{0pt}
\fancypagestyle{plain}{%
\fancyhead{}
\renewcommand{\headrulewidth}{0pt}
}

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Appendix caption style                      %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%figures
\makeatletter
\g@addto@macro\appendix{\renewcommand{\thefigure}{A.\arabic{figure}}\setcounter{figure}{0}}
\makeatother

%tables
\makeatletter
\g@addto@macro\appendix{\renewcommand{\thetable}{A.\arabic{table}}\setcounter{table}{0}}
\makeatother

%codes
\makeatletter
\g@addto@macro\appendix{\renewcommand{\thelstlisting}{A.\arabic{lstlisting}}\setcounter{lstlisting}{0}}
\makeatother


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Language settings                           %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

\renewcommand{\listtheoremname}{Lista de Teoremas}
\newcommand{\nombrelistadefinicion}{Lista de Definiciones}
\newcommand{\nombrelistalema}{Lista de Lemas}
