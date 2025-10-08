If InitSprite()
Else
MessageRequester("Blocks","DirectX v.7 or later was unable to be initialized",#MB_ICONERROR)
End
EndIf
If InitKeyboard()
Else
MessageRequester("Blocks","A keyboard was unable to be initialized!",#MB_ICONERROR)
End
EndIf
If InitSound()
UseOGGSoundDecoder()
Else
MessageRequester("Blocks","Sound was unable to be initialized!",#MB_ICONERROR)
End
EndIf

If InitJoystick()
joystick = 1
Else
joystick = 0
EndIf


OnErrorResume()

Enumeration ; sprites
#Block = 1;
#BorderL = 30
#BorderH
#BorderTC
#BorderV
#BorderBC
#Next
#Numbers = 40
#Letters = 50
EndEnumeration

Enumeration ; sound
#Line = 1
#Tetris
#Blk
#Rotate
#lost
#pause
#musicA
#musicB
#musicC
EndEnumeration

Procedure BitmapText(Text$,x,y)

Text$ = UCase(Text$)
Length = Len(Text$)
For k=1 To Length
A$ = Mid(Text$, k,1)
If A$=" "
x+20
Else
v = Asc(A$)
If v>=65 And v<=90
  v = #Letters + v - 65 ; 65 ASCII 'A'
  ElseIf v>=48 And v<=57
  v = #Numbers + v - 48 ; 48 ASCII '0'
  ElseIf v=33
  v =  #Letters+26 ; 33 ASCII '!'
  EndIf
  If IsSprite(v)
  DisplayTransparentSprite(v,x,y)
  x+SpriteWidth(v)
  EndIf
  EndIf
  Next
  EndProcedure

  Procedure.s getName()
  name$ = InputRequester("Blocks - You have a new Hi Score!","Enter your name","")
  name$ = Trim(name$)
  If Len(name$)>10
name$ = Mid(name$,1,10)
EndIf
ProcedureReturn name$
EndProcedure

Define.l score, highscore

Structure piece
image.w
b1x.w
b1y.w
b2x.w
b2y.w
b3x.w
b3y.w
b4x.w
b4y.w
b5x.w
b5y.w
b6x.w
b6y.w
EndStructure

np.piece
cp.piece

Width = 15
Height = 25
Dim blocks.b(Width, Height)


DataSection     ; ********* a includebinary file bigger .exe but noone can steal stuff :)

blk1: IncludeBinary "images\1.bmp"
blk2: IncludeBinary "images\2.bmp"
blk3: IncludeBinary "images\3.bmp"
blk4: IncludeBinary "images\4.bmp"
blk5: IncludeBinary "images\5.bmp"
blk6: IncludeBinary "images\6.bmp"
blk7: IncludeBinary "images\7.bmp"
blk8: IncludeBinary "images\8.bmp"
blk9: IncludeBinary "images\9.bmp"
blk10: IncludeBinary "images\10.bmp"
blk11: IncludeBinary "images\11.bmp"
blk12: IncludeBinary "images\12.bmp"
blk13: IncludeBinary "images\13.bmp"
blk14: IncludeBinary "images\14.bmp"
blk15: IncludeBinary "images\15.bmp"
blk16: IncludeBinary "images\16.bmp"
blk17: IncludeBinary "images\17.bmp"
blk18: IncludeBinary "images\18.bmp"
blk19: IncludeBinary "images\19.bmp"
blk20: IncludeBinary "images\20.bmp"
blk21: IncludeBinary "images\21.bmp"
blk22: IncludeBinary "images\22.bmp"
blk23: IncludeBinary "images\23.bmp"
blk24: IncludeBinary "images\24.bmp"
blk25: IncludeBinary "images\25.bmp"
blk26: IncludeBinary "images\26.bmp"
blk27: IncludeBinary "images\27.bmp"
blk28: IncludeBinary "images\28.bmp"
blk29: IncludeBinary "images\29.bmp"

bdrL: IncludeBinary "images\bdrL.bmp"
bdrH: IncludeBinary "images\bdrH.bmp"
bdrTC: IncludeBinary "images\bdrTC.bmp"
bdrV: IncludeBinary "images\bdrV.bmp"
bdrBC: IncludeBinary "images\bdrBC.bmp"

Nxt: IncludeBinary "images\next.bmp"

a: IncludeBinary "images\Font\A.bmp"
b: IncludeBinary "images\Font\B.bmp"
c: IncludeBinary "images\Font\C.bmp"
d: IncludeBinary "images\Font\D.bmp"
e: IncludeBinary "images\Font\E.bmp"
f: IncludeBinary "images\Font\F.bmp"
g: IncludeBinary "images\Font\G.bmp"
h: IncludeBinary "images\Font\H.bmp"
i: IncludeBinary "images\Font\I.bmp"
j: IncludeBinary "images\Font\J.bmp"
k: IncludeBinary "images\Font\K.bmp"
l: IncludeBinary "images\Font\L.bmp"
m: IncludeBinary "images\Font\M.bmp"
n: IncludeBinary "images\Font\N.bmp"
o: IncludeBinary "images\Font\O.bmp"
p: IncludeBinary "images\Font\P.bmp"
q: IncludeBinary "images\Font\Q.bmp"
r: IncludeBinary "images\Font\R.bmp"
s: IncludeBinary "images\Font\S.bmp"
t: IncludeBinary "images\Font\T.bmp"
u: IncludeBinary "images\Font\U.bmp"
v: IncludeBinary "images\Font\V.bmp"
w: IncludeBinary "images\Font\W.bmp"
x: IncludeBinary "images\Font\X.bmp"
y: IncludeBinary "images\Font\Y.bmp"
z: IncludeBinary "images\Font\Z.bmp"
exclam: IncludeBinary "images\Font\!.bmp"

0: IncludeBinary "images\Font\0.bmp"
1: IncludeBinary "images\Font\1.bmp"
2: IncludeBinary "images\Font\2.bmp"
3: IncludeBinary "images\Font\3.bmp"
4: IncludeBinary "images\Font\4.bmp"
5: IncludeBinary "images\Font\5.bmp"
6: IncludeBinary "images\Font\6.bmp"
7: IncludeBinary "images\Font\7.bmp"
8: IncludeBinary "images\Font\8.bmp"
9: IncludeBinary "images\Font\9.bmp"


tetris: IncludeBinary "sound\tetris.wav"
line: IncludeBinary "sound\one_line.wav"
blk: IncludeBinary "sound\block.wav"
rotateS: IncludeBinary "sound\rotate.wav"
lost: IncludeBinary "sound\lost.wav"
pause: IncludeBinary "sound\pause.wav"
AA: IncludeBinary "sound\A.ogg" :AAA:
    BB: IncludeBinary "sound\B.ogg" :BBB:
    CC: IncludeBinary "sound\C.ogg" :CCC:
    EndDataSection


CatchSound(#Tetris, ?tetris)
CatchSound(#Line, ?line)
CatchSound(#Blk, ?Blk)
CatchSound(#Rotate, ?rotateS)
CatchSound(#lost, ?lost)
CatchSound(#pause, ?pause)

CatchSound(#musicA, ?AA, ?AAA - ?AA)
CatchSound(#musicB, ?BB, ?BBB - ?BB)
CatchSound(#musicC, ?CC, ?CCC - ?CC)

BackGround = #musicA

screenX = Width*24+195
screenY = Height*24+48-3
If OpenWindow(0,200,50,screenX,screenY,"Blocks",#PB_Window_SystemMenu | #PB_Window_MinimizeGadget| #PB_Window_MaximizeGadget | #PB_Window_SizeGadget)

Enumeration
#M_Baby
#M_Normal
#M_Blocks
#M_SizeS
#M_SizeM
#M_SizeB
#M_Reset
#M_Level
#M_StartHeight
#M_ResetHiScore
#M_Controls
#M_ViewHiScores
#M_About
#M_Exit
#M_MusicA
#M_MusicB
#M_MusicC
#M_MusicD
EndEnumeration

If CreateMenu(0, WindowID(0))
MenuTitle("Mode")
MenuItem(#M_Baby, "Baby")
MenuItem(#M_Normal, "Normal")
MenuItem(#M_Blocks, "Blocks")
MenuTitle("Size")
MenuItem(#M_SizeS, "Small  10x10")
MenuItem(#M_SizeM, "Medium 10x20")
MenuItem(#M_SizeB, "Big 15x25")
MenuTitle("Options")
MenuItem(#M_Reset, "Reset")
MenuItem(#M_Level, "Choose Level")
MenuItem(#M_StartHeight, "Starting Height")
MenuItem(#M_ResetHiScore, "Reset HiScore")
MenuItem(#M_Controls, "View Controls")
MenuItem(#M_ViewHiScores, "View HiScores")
MenuItem(#M_About, "About")
MenuBar()
MenuItem( #M_Exit, "Exit")
MenuTitle("Sound")
MenuItem(#M_MusicA, "Music A")
MenuItem(#M_MusicB, "Music B")
MenuItem(#M_MusicC, "Music C")
MenuItem(#M_MusicD, "None")
EndIf

; open screen bigger than window that way the play area can be adjusted
If OpenWindowedScreen(WindowID(0),0,0,screenX+240,screenY+240,0,0,0)
CatchSprite(#Block,?blk1,0)
CatchSprite(#Block+1,?blk2,0)
CatchSprite(#Block+2,?blk3,0)
CatchSprite(#Block+3,?blk4,0)
CatchSprite(#Block+4,?blk5,0)
CatchSprite(#Block+5,?blk6,0)
CatchSprite(#Block+6,?blk7,0)
CatchSprite(#Block+7,?blk8,0)
CatchSprite(#Block+8,?blk9,0)
CatchSprite(#Block+9,?blk10,0)
CatchSprite(#Block+10,?blk11,0)
CatchSprite(#Block+11,?blk12,0)
CatchSprite(#Block+12,?blk13,0)
CatchSprite(#Block+13,?blk14,0)
CatchSprite(#Block+14,?blk15,0)
CatchSprite(#Block+15,?blk16,0)
CatchSprite(#Block+16,?blk17,0)
CatchSprite(#Block+17,?blk18,0)
CatchSprite(#Block+18,?blk19,0)
CatchSprite(#Block+19,?blk20,0)
CatchSprite(#Block+20,?blk21,0)
CatchSprite(#Block+21,?blk22,0)
CatchSprite(#Block+22,?blk23,0)
CatchSprite(#Block+23,?blk24,0)
CatchSprite(#Block+24,?blk25,0)
CatchSprite(#Block+25,?blk26,0)
CatchSprite(#Block+26,?blk27,0)
CatchSprite(#Block+27,?blk28,0)
CatchSprite(#Block+28,?blk29,0)

CatchSprite(#BorderL,?bdrL,0)
CatchSprite(#BorderH,?bdrH,0)
CatchSprite(#BorderTC,?bdrTC,0)
CatchSprite(#BorderV,?bdrV,0)
CatchSprite(#BorderBC,?bdrBC,0)

CatchSprite(#Next,?Nxt,0)

CatchSprite(#Letters+0,?a,0)
CatchSprite(#Letters+1,?b,0)
CatchSprite(#Letters+2,?c,0)
CatchSprite(#Letters+3,?d,0)
CatchSprite(#Letters+4,?e,0)
CatchSprite(#Letters+5,?f,0)
CatchSprite(#Letters+6,?g,0)
CatchSprite(#Letters+7,?h,0)
CatchSprite(#Letters+8,?i,0)
CatchSprite(#Letters+9,?j,0)
CatchSprite(#Letters+10,?k,0)
CatchSprite(#Letters+11,?l,0)
CatchSprite(#Letters+12,?m,0)
CatchSprite(#Letters+13,?n,0)
CatchSprite(#Letters+14,?o,0)
CatchSprite(#Letters+15,?p,0)
CatchSprite(#Letters+16,?q,0)
CatchSprite(#Letters+17,?r,0)
CatchSprite(#Letters+18,?s,0)
CatchSprite(#Letters+19,?t,0)
CatchSprite(#Letters+20,?u,0)
CatchSprite(#Letters+21,?v,0)
CatchSprite(#Letters+22,?w,0)
CatchSprite(#Letters+23,?x,0)
CatchSprite(#Letters+24,?y,0)
CatchSprite(#Letters+25,?z,0)
CatchSprite(#Letters+26,?exclam,0)

CatchSprite(#Numbers+0,?0,0)
CatchSprite(#Numbers+1,?1,0)
CatchSprite(#Numbers+2,?2,0)
CatchSprite(#Numbers+3,?3,0)
CatchSprite(#Numbers+4,?4,0)
CatchSprite(#Numbers+5,?5,0)
CatchSprite(#Numbers+6,?6,0)
CatchSprite(#Numbers+7,?7,0)
CatchSprite(#Numbers+8,?8,0)
CatchSprite(#Numbers+9,?9,0)

;Mode = 1 ; NORMAL
Mode = 2; BLOCK
Gosub LoadHiScore
musicPlaying = 1
startHeight = 0
Gosub newGame

Repeat

Gosub ExamineKeyboard
If ElapsedMilliseconds() - time >=timeD
timeD = LevelSpeed
time = ElapsedMilliseconds()
Gosub dropPiece
Gosub Draw
EndIf
Gosub GetEvents
Until playing <> 1 Or  event = #PB_Event_CloseWindow Or KeyboardPushed(#PB_Key_Escape)

Goto GameEnd

Else
MessageRequester("Tetris","A Screen unable to be initialized!",#MB_ICONERROR)
EndIf
Else
MessageRequester("Tetris","A window was unable to be opened!",#MB_ICONERROR)
EndIf

; shouldn't get here.... but if it does
Goto GameEnd


;**********************************************************
;
;- Draw
;
;**********************************************************
Draw:
    ClearScreen(RGB(0,0,0))
Gosub DrawPieces
Gosub DrawMenu
If TetrisCtr > 0
bitmapText(TetrisMsg$,(Width*24)/2-50,(Height-1)*24/2)
TetrisCtr-1
EndIf
FlipBuffers()
Return


;**********************************************************
;
;- ExamineKeyboard
;
;**********************************************************
ExamineKeyboard:
    ExamineKeyboard()
If joystick = 1
ExamineJoystick()
EndIf

If ElapsedMilliseconds() - Movetime >= moveD
Movetime = ElapsedMilliseconds()

If KeyboardPushed(#PB_Key_Left) Or (joystick = 1 And JoystickAxisX() = -1 )
If PieceY <= Height
  If PieceX+cp\b1x-1 >= 0 And PieceX+cp\b2x-1 >= 0 And PieceX+cp\b3x-1 >= 0 And PieceX+cp\b4x-1 >= 0 And PieceX+cp\b5x-1 >= 0 And PieceX+cp\b6x-1 >= 0 And PieceY+cp\b1y > 0 And PieceY+cp\b2y > 0 And PieceY+cp\b3y > 0 And PieceY+cp\b4y > 0 And PieceY+cp\b5y > 0 And PieceY+cp\b6y > 0
If blocks(PieceX + cp\b1x-1, PieceY + cp\b1y) = 0 And blocks(PieceX + cp\b2x-1, PieceY + cp\b2y) = 0 And blocks(PieceX + cp\b3x-1, PieceY + cp\b3y) = 0 And blocks(PieceX + cp\b4x-1, PieceY + cp\b4y) = 0 And blocks(PieceX + cp\b5x-1, PieceY + cp\b5y) = 0 And blocks(PieceX + cp\b6x-1, PieceY + cp\b6y) = 0
PieceX-1
EndIf
EndIf
EndIf
Gosub Draw
EndIf


If  KeyboardPushed(#PB_Key_Right) Or (joystick = 1 And JoystickAxisX() = 1 )
If PieceY <= Height
  If PieceX+cp\b1x+1 < Width And PieceX+cp\b2x+1 < Width And PieceX+cp\b3x+1 < Width And PieceX+cp\b4x+1 < Width And PieceX+cp\b5x+1 < Width And PieceX+cp\b6x+1 < Width  And PieceY+cp\b1y > 0 And PieceY+cp\b2y > 0 And PieceY+cp\b3y > 0 And PieceY+cp\b4y > 0 And PieceY+cp\b5y > 0 And PieceY+cp\b6y > 0
If blocks(PieceX + cp\b1x+1, PieceY + cp\b1y) = 0 And blocks(PieceX + cp\b2x+1, PieceY + cp\b2y) = 0 And blocks(PieceX + cp\b3x+1, PieceY + cp\b3y) = 0 And blocks(PieceX + cp\b4x+1, PieceY + cp\b4y) = 0 And blocks(PieceX + cp\b5x+1, PieceY + cp\b5y) = 0 And blocks(PieceX + cp\b6x+1, PieceY + cp\b6y) = 0
PieceX+1
EndIf
EndIf
EndIf
Gosub Draw
EndIf
If  KeyboardPushed(#PB_Key_Down) Or (joystick = 1 And JoystickAxisY() = 1 )
Gosub Draw
timeD = 0
EndIf
EndIf

If ElapsedMilliseconds() - Fliptime >= flipD
Fliptime = ElapsedMilliseconds()

If KeyboardPushed(#PB_Key_X) Or (joystick = 1 And (JoystickButton(4) Or  JoystickButton(1) Or JoystickButton(8) ) )
Gosub rotateL
Gosub Draw
EndIf
If KeyboardPushed(#PB_Key_C) Or (joystick = 1 And (JoystickButton(3) Or  JoystickButton(2) Or JoystickButton(7) ) )
Gosub rotateR
Gosub Draw
EndIf

EndIf

If KeyboardPushed(#PB_Key_R) Or (joystick = 1 And JoystickButton(9) )
Gosub newgame
Gosub Draw
EndIf


Repeat
If WindowID(0)
Event = WindowEvent()
If Event
Delay(10)
EndIf
EndIf

Gosub getEvents

ExamineKeyboard()
If KeyboardReleased(#PB_Key_RightAlt) Or KeyboardReleased(#PB_Key_Up);pause
If Paused = 0
ClearScreen(RGB(0,0,0))
Gosub DrawMenu
BitmapText("PAUSE",(Width*24)/2-50,(Height-1)*24/2)
FlipBuffers()
Paused = 1
PlaySound(#pause)
Else
Paused = 0
EndIf
EndIf
Until paused = 0

Gosub getEvents
If joystick = 1
If JoystickButton(10)
ClearScreen(RGB(0,0,0))
Gosub DrawMenu
BitmapText("PAUSE",(Width*24)/2-50,(Height-1)*24/2)
FlipBuffers()
Paused = 1
PlaySound(#pause)
FlipBuffers()
EndIf
Repeat
If joystick = 1
ExamineJoystick()
EndIf
If pausecounter < 0
  If joystick = 1
  If JoystickButton(1) Or JoystickButton(2) Or JoystickButton(3) Or JoystickButton(4) Or JoystickButton(5) Or JoystickButton(6) Or JoystickButton(7) Or JoystickButton(8)
  paused = 0
  pausecounter = 20
  EndIf
  EndIf
  Else
  pausecounter - 1
  EndIf
  Until paused = 0
  EndIf

  Return



  ;**********************************************************
  ;
  ;- GetEvents
  ;
  ;**********************************************************
  GetEvents:
      EventID.l = WindowEvent()

  Select EventID
  Case #PB_Event_CloseWindow
  playing = 0
  Paused = 0
  Case #PB_Event_Menu      ; A Menu item has been selected
  Select EventMenu()

  Case   #M_Baby
  If mode <> 0
  Mode = 0
  Gosub newGame
  Paused = 0
  Else
  MessageRequester("Block","You are playing Baby Mode.",0)
  EndIf

  Case   #M_Normal
  If mode <> 1
  Mode = 1
  Gosub newGame
  Paused = 0
  Else
  MessageRequester("Block","You are playing Normal Mode.",0)
  EndIf

  Case   #M_Blocks
  If mode <> 2
  Mode = 2
  Gosub newGame
  Paused = 0
  Else
  MessageRequester("Block","You are playing Block Mode.",0)
  EndIf

  Case   #M_SizeS
  Width = 10
  Height = 10
  Gosub newGame
  Paused = 0
  ResizeWindow(0,WindowX(0),WindowY(0),Width*24+195,Height*2*24+48-3)

  Case   #M_SizeM
  Width = 10
  Height = 20
  Gosub newGame
  Paused = 0
  ResizeWindow(0,WindowX(0),WindowY(0),Width*24+195,Height*24+48-3)

  Case   #M_SizeB
  Width = 15
  Height = 25
  Gosub newGame
  Paused = 0
  ResizeWindow(0,WindowX(0),WindowY(0),Width*24+195,Height*24+48-3)


  Case   #M_Reset
  Result = MessageRequester("Block","Are you sure that you want to Restart? "+var$,#PB_MessageRequester_YesNo)
  If Result = #PB_MessageRequester_Yes
  t = Mode
  Gosub newGame
  Mode = t
  Paused = 0
  EndIf

  Case   #M_Level
  var$ = InputRequester("Blocks","Choose your level 0-9","0")
  If Val(var$) >= 0 And Val(var$) <= 9
Result = MessageRequester("Block","Are you Sure? level "+var$,#PB_MessageRequester_YesNo)
If Result = #PB_MessageRequester_Yes
Gosub newGame
level = Val(var$)
levelSpeed = levelSpeed - 25*level
If levelSpeed < 25
  levelSpeed = 25
  EndIf
  timeD = levelSpeed
  Paused = 0
  EndIf
  Else
  MessageRequester("Blocks","Not a valid Selection",0)
  EndIf

  Case   #M_StartHeight
  s$ = InputRequester("Blocks","Input starting height 0 -"+Str(Height-5),"0")
  If Val(s$) >= 0 And Val(s$) <= Height-5
Gosub newGame
Paused = 0
For y = Height-1 To Height - Val(s$) Step -1
k = 0
For x = 0 To Width-1
If Random(5) >=4 And k < Width/2
  If mode <> 2
  blocks(x,y) = Random(7)+1
  Else
  blocks(x,y) = Random(26)+1
  EndIf
  k+1
  EndIf
  Next
  Next
  Else
  MessageRequester("Blocks","Invalid choice",0)
  EndIf

  Case #M_Controls
  MessageRequester("Blocks","Controls"+Chr(13)+"--------"+Chr(13)+"X - Rotate Counterclockwise"+Chr(13)+"C - Rotate Counterclockwise"+Chr(13)+"Right Alt/ Up Arrow - Pause"+Chr(13)+"Left & Right Arrows - Move Pieces"+Chr(13)+"Down Arrow - Speed Fall"+Chr(13)+"R - Reset"+Chr(13)+"Escape - Exit",0)

  Case #M_ViewHiScores
  MessageRequester("Blocks","Baby Mode"+Chr(13)+"--------"+Chr(13)+"Small     "+hiscoreN$+": "+Str(hiscore)+Chr(13)+"Medium  "+hiscoreMN$+": "+Str(hiscoreM)+Chr(13)+"Big        "+hiscoreBN$+": "+Str(hiscoreB)+Chr(13)+Chr(13)+"Normal Mode"+Chr(13)+"--------"+Chr(13)+"Small     "+hiscore2N$+": "+Str(hiscore2)+Chr(13)+"Medium  "+hiscore2MN$+": "+Str(hiscore2M)+Chr(13)+"Big        "+hiscore2BN$+": "+Str(hiscore2B)+Chr(13)+Chr(13)+"Blocks Mode"+Chr(13)+"--------"+Chr(13)+"Small     "+hiscore3N$+": "+Str(hiscore3)+Chr(13)+"Medium  "+hiscore3MN$+": "+Str(hiscore3M)+Chr(13)+"Big        "+hiscore3BN$+": "+Str(hiscore3B))

  Case #M_About
  MessageRequester("Blocks","Programmed by: Kenneth Cason"+Chr(13)+"Email: reddragon72455@yahoo.com",0)

  Case #M_ResetHiScore
  temp = score
  score = 0
  If Height = 10; Baby
  If mode = 0
  hiscore = 0
  hiscoreN$ = ""
  ElseIf mode = 1
  hiscore2 = 0
  hiscore2N$ = ""
  Else
  hiscore3 = 0
  hiscore3N$ = ""
  EndIf
  ElseIf Height = 20; Normal
  If mode = 0
  hiscoreM = 0
  hiscoreMN$ = ""
  ElseIf mode = 1
  hiscore2M = 0
  hiscore2MN$ = ""
  Else
  hiscore3M = 0
  hiscore3MN$ = ""
  EndIf
  ElseIf Height = 25; Blocks
  If mode = 0
  hiscoreB = 0
  hiscoreBN$ = ""
  ElseIf mode = 1
  hiscore2B = 0
  hiscore2BN$ = ""
  Else
  hiscore3B = 0
  hiscore3BN$ = ""
  EndIf
  EndIf
  Gosub saveHiScore
  score = temp

  Case #M_Exit
  playing = 0
  Paused = 0

  Case #M_MusicA
  If IsSound(BackGround)
  If musicPlaying = 1
  StopSound(BackGround)
  EndIf
  StopSound(BackGround)
  BackGround = #musicA
  PlaySound(BackGround,1)
  musicPlaying = 1
  EndIf

  Case #M_MusicB
  If IsSound(BackGround)
  If musicPlaying = 1
  StopSound(BackGround)
  EndIf
  StopSound(BackGround)
  BackGround = #musicB
  PlaySound(BackGround,1)
  musicPlaying = 1
  EndIf

  Case #M_MusicC
  If IsSound(BackGround)
  If musicPlaying = 1
  StopSound(BackGround)
  EndIf
  BackGround = #musicC
  PlaySound(BackGround,1)
  musicPlaying = 1
  EndIf

  Case #M_MusicD
  If IsSound(BackGround)
  musicPlaying = 0
  StopSound(BackGround)
  EndIf

  EndSelect
  EndSelect
  Return


  ;**********************************************************
  ;
  ;- DrawMenu
  ;
  ;**********************************************************
  DrawMenu:
  ;********* Draw Border
  DisplaySprite(#BorderL, 0, 0)
  For k = 1 To Width
  DisplaySprite(#BorderH, k*24, 0)
  Next
  DisplaySprite(#BorderTC, Width*24, 0)
  For k = 1 To Height-1
  DisplaySprite(#BorderV, Width*24, k*24)
  Next
  DisplaySprite(#BorderBC, Width*24, Height*24)
  DisplaySprite(#BorderL, 0, Height*24)
  For k = 1 To Width-1
  DisplaySprite(#BorderH, k*24, Height*24)
  Next
  ;*********
  OffsetX = Width*24+24
  bitmapText("BLOCKS",OffsetX,10)
  bitmapText("NEXT",OffsetX,35)
  DisplaySprite(#Next,OffsetX,62)
  DisplaySprite(np\image,OffsetX+60+np\b1x*24+(4*np\b1x),94+np\b1y*24+(4*np\b1y))
  DisplaySprite(np\image,OffsetX+60+np\b2x*24+(4*np\b2x),94+np\b2y*24+(4*np\b2y))
  DisplaySprite(np\image,OffsetX+60+np\b3x*24+(4*np\b3x),94+np\b3y*24+(4*np\b3y))
  DisplaySprite(np\image,OffsetX+60+np\b4x*24+(4*np\b4x),94+np\b4y*24+(4*np\b4y))
  DisplaySprite(np\image,OffsetX+60+np\b5x*24+(4*np\b5x),94+np\b5y*24+(4*np\b5y))
  DisplaySprite(np\image,OffsetX+60+np\b6x*24+(4*np\b6x),94+np\b6y*24+(4*np\b6y))
  bitmapText("MODE",OffsetX,155)
  If mode = 0
  bitmapText(" BABY",OffsetX,180)
  ElseIf mode = 1
  bitmapText(" NORMAL",OffsetX,180)
  ElseIf mode = 2
  bitmapText(" BLOCKS",OffsetX,180)
  EndIf
  bitmapText("LEVEL",OffsetX,205)
  bitmapText(" "+Str(level),OffsetX,230)
  bitmapText("LINES",OffsetX,255)
  bitmapText(" "+Str(lines),OffsetX,280)
  bitmapText("SCORE",OffsetX,305)
  bitmapText(" "+Str(score),OffsetX,330)
  bitmapText("HISCORE",OffsetX,355)
  bitmapText(" "+Str(hiscoreThisMode),OffsetX,380)
  bitmapText(" "+hiscoreThisModeN$,OffsetX,405)

  Return


  ;**********************************************************
  ;
  ;- rotateL
  ;
  ;**********************************************************
  rotateL:

      If CurrentPiece > 0 ; not a square
If PieceX + cp\b1y >= 0 And PieceX + cp\b2y >= 0 And PieceX + cp\b3y >= 0 And PieceX + cp\b4y >= 0  And PieceX + cp\b5y >= 0  And PieceX + cp\b6y >= 0 And PieceX + cp\b1y < Width And PieceX + cp\b2y < Width And PieceX + cp\b3y < Width And PieceX + cp\b4y < Width  And PieceX + cp\b5y < Width And PieceX + cp\b6y < Width  And PieceY - cp\b1x < Height And PieceY - cp\b2x < Height And PieceY - cp\b3x < Height And PieceY - cp\b4x < Height  And PieceY - cp\b5x < Height  And PieceY - cp\b6x < Height And PieceY - cp\b1x >= 0 And PieceY - cp\b2x >= 0  And PieceY - cp\b3x >= 0  And PieceY - cp\b4x >= 0 And PieceY - cp\b5x >= 0 And PieceY - cp\b6x >= 0 ; flip is in bounds
  If blocks(PieceX + cp\b1y, PieceY - cp\b1x) = 0 And blocks(PieceX + cp\b2y, PieceY - cp\b2x) = 0 And blocks(PieceX + cp\b3y, PieceY - cp\b3x) = 0 And blocks(PieceX + cp\b4y, PieceY - cp\b4x) = 0 And blocks(PieceX + cp\b5y, PieceY - cp\b5x) = 0 And blocks(PieceX + cp\b6y, PieceY - cp\b6x) = 0
  t = cp\b1x
  cp\b1x = cp\b1y
  cp\b1y=-t
  t = cp\b2x
  cp\b2x = cp\b2y
  cp\b2y=-t
  t = cp\b3x
  cp\b3x = cp\b3y
  cp\b3y=-t
  t = cp\b4x
  cp\b4x = cp\b4y
  cp\b4y=-t
  t = cp\b5x
  cp\b5x = cp\b5y
  cp\b5y=-t
  t = cp\b6x
  cp\b6x = cp\b6y
  cp\b6y=-t
  EndIf
  EndIf
  EndIf
  Return

  ;**********************************************************
  ;
  ;- rotateR
  ;
  ;**********************************************************
  rotateR:

      If CurrentPiece > 0 ; not a square
  If PieceX - cp\b1y >= 0 And PieceX - cp\b2y >= 0 And PieceX - cp\b3y >= 0 And PieceX - cp\b4y >= 0  And PieceX - cp\b5y >= 0  And PieceX - cp\b6y >= 0 And PieceX - cp\b1y < Width And PieceX - cp\b2y < Width And PieceX - cp\b3y < Width And PieceX - cp\b4y < Width  And PieceX - cp\b5y < Width And PieceX - cp\b6y < Width  And PieceY + cp\b1x < Height And PieceY + cp\b2x < Height And PieceY + cp\b3x < Height And PieceY + cp\b4x < Height  And PieceY + cp\b5x < Height  And PieceY + cp\b6x < Height And PieceY + cp\b1x >= 0 And PieceY + cp\b2x >= 0  And PieceY + cp\b3x >= 0  And PieceY + cp\b4x >= 0 And PieceY + cp\b5x >= 0 And PieceY + cp\b6x >= 0 ; flip is in bounds
  If blocks(PieceX - cp\b1y, PieceY + cp\b1x) = 0 And blocks(PieceX - cp\b2y, PieceY + cp\b2x) = 0 And blocks(PieceX - cp\b3y, PieceY + cp\b3x) = 0 And blocks(PieceX - cp\b4y, PieceY + cp\b4x) = 0 And blocks(PieceX - cp\b5y, PieceY + cp\b5x) = 0 And blocks(PieceX - cp\b6y, PieceY + cp\b6x) = 0
  t = cp\b1x
  cp\b1x = -cp\b1y
  cp\b1y=t
  t = cp\b2x
  cp\b2x = -cp\b2y
  cp\b2y=t
  t = cp\b3x
  cp\b3x = -cp\b3y
  cp\b3y=t
  t = cp\b4x
  cp\b4x = -cp\b4y
  cp\b4y=t
  t = cp\b5x
  cp\b5x = -cp\b5y
  cp\b5y=t
  t = cp\b6x
  cp\b6x = -cp\b6y
  cp\b6y=t
  EndIf
  EndIf
  EndIf
  Return

  ;**********************************************************
  ;
  ;- dropPiece
  ;
  ;**********************************************************
  dropPiece:
      doneFalling = 0
  If (PieceY + cp\b1y+1) < Height And (PieceY + cp\b2y+1) < Height And (PieceY + cp\b3y+1) < Height And (PieceY + cp\b4y+1) < Height And (PieceY + cp\b5y+1) < Height  And (PieceY + cp\b6y+1) < Height
  If blocks(PieceX + cp\b1x, PieceY + cp\b1y+1) = 0 And blocks(PieceX + cp\b2x, PieceY + cp\b2y+1) = 0  And blocks(PieceX + cp\b3x, PieceY + cp\b3y+1) = 0 And blocks(PieceX + cp\b4x, PieceY + cp\b4y+1) = 0  And blocks(PieceX + cp\b5x, PieceY + cp\b5y+1) = 0 And blocks(PieceX + cp\b6x, PieceY + cp\b6y+1) = 0
  PieceY+1
  Else
  doneFalling = 1
  EndIf
  Else
  doneFalling = 1
  EndIf

  If doneFalling = 1
  PlaySound(#Blk)
  If PieceY + cp\b1y >= 0
  blocks(PieceX + cp\b1x, PieceY + cp\b1y) = cp\image
  EndIf
  If PieceY + cp\b2y >= 0
  blocks(PieceX + cp\b2x, PieceY + cp\b2y) = cp\image
  EndIf
  If PieceY + cp\b3y >= 0
  blocks(PieceX + cp\b3x, PieceY + cp\b3y) = cp\image
  EndIf
  If PieceY + cp\b4y >= 0
  blocks(PieceX + cp\b4x, PieceY + cp\b4y) = cp\image
  EndIf
  If PieceY + cp\b5y >= 0
  blocks(PieceX + cp\b5x, PieceY + cp\b5y) = cp\image
  EndIf
  If PieceY + cp\b6y >= 0
blocks(PieceX + cp\b6x, PieceY + cp\b6y) = cp\image
EndIf
score+30+10*level
Gosub setCurrentPiece
Gosub getNextPiece
PieceX = Width/2
PieceY = 0
EndIf
Gosub isLine
Gosub isDead
Return

;**********************************************************
;
;- DrawPieces
;
;**********************************************************
DrawPieces:
    For x = 0 To Width-1
For y = 0 To Height-1
If Blocks(x,y) > 0
DisplaySprite(blocks(x,y), x*24, y*24)
EndIf
Next
Next
If PieceY + cp\b1y >= 0
DisplaySprite(cp\image, (PieceX + cp\b1x)*24, (PieceY + cp\b1y)*24)
EndIf
If PieceY + cp\b1y >= 0
DisplaySprite(cp\image, (PieceX + cp\b2x)*24, (PieceY + cp\b2y)*24)
EndIf
If PieceY + cp\b1y >= 0
DisplaySprite(cp\image, (PieceX + cp\b3x)*24, (PieceY + cp\b3y)*24)
EndIf
If PieceY + cp\b1y >= 0
DisplaySprite(cp\image, (PieceX + cp\b4x)*24, (PieceY + cp\b4y)*24)
EndIf
If PieceY + cp\b1y >= 0
DisplaySprite(cp\image, (PieceX + cp\b5x)*24, (PieceY + cp\b5y)*24)
EndIf
If PieceY + cp\b1y >= 0
DisplaySprite(cp\image, (PieceX + cp\b6x)*24, (PieceY + cp\b6y)*24)
EndIf
Return

;**********************************************************
;
;- isLine
;
;**********************************************************
isLine:
    lineCtr = 0
For y = 0 To Height-1
blockCtr = 0
For x = 0 To Width-1
If Blocks(x,y) > 0
blockCtr + 1
If blockCtr = Width
lines + 1
lineCtr + 1
For k = 0 To Width-1 ; delete Row
blocks(k,y) = 0
Next
For x = 0 To Width-1 ; drop Lines
For j = y To 1 Step -1
blocks(x,j)= blocks(x,j-1)
Next
Next
EndIf
EndIf
Next
Next
TetrisLine = 0
If lineCtr > 0 And lineCtr < 4
  If lineCtr = 3 And mode = 0
  score+2500
  tetrisCtr = 30
  TetrisLine = 3
  TetrisMsg$ = "WHAMMO!"
  PlaySound(#Tetris)
  Else
  score+150+200*lineCtr
  PlaySound(#Line)
  EndIf
  ElseIf lineCtr = 4 ; BLOCK!!!
  tetrisCtr = 30
  score+5000
  TetrisLine = 4
  If mode = 0
  TetrisMsg$ = "GOOD!"
  Else
  TetrisMsg$ = "AWESOME!"
  EndIf
  PlaySound(#Tetris)
  ElseIf lineCtr = 5 ; BLOCK!!!
  tetrisCtr = 30
  score+10000
  TetrisLine = 5
  TetrisMsg$ = "ALMOST!"
  PlaySound(#Tetris)
  ElseIf lineCtr >= 6 ; BLOCK!!!
tetrisCtr = 30
score+15000
TetrisLine = 6
TetrisMsg$ = "BLOCK!!"
PlaySound(#Tetris)
EndIf

If lines >= level*10+10
level + 1
If levelSpeed >=75
levelSpeed-25
ElseIf levelSpeed > 30
levelSpeed-2
EndIf
If levelSpeed < 30
  levelSpeed = 30
  EndIf
  EndIf
  Return


  ;**********************************************************
  ;
  ;- isDead
  ;
  ;**********************************************************
  isDead:
      For x = 0 To Width-1
  If blocks(x,0) > 0
Gosub saveHiScore
If IsSound(BackGround)
StopSound(BackGround)
EndIf
PlaySound(#lost)
Gosub newGame
Delay(2000)
EndIf
Next

Return

;**********************************************************
;
;- newGame
;
;**********************************************************
newGame:
; get hiscore
If Height = 10 ; Baby Size
If mode = 0
hiscoreThisMode = hiscore
hiscoreThisModeN$ = hiscoreN$
ElseIf mode = 1
hiscoreThisMode = hiscore2
hiscoreThisModeN$ = hiscore2N$
ElseIf mode = 2
hiscoreThisMode = hiscore3
hiscoreThisModeN$ = hiscore3N$
EndIf
ElseIf Height = 20 ; Normal Size
If mode = 0
hiscoreThisMode = hiscoreM

hiscoreThisModeN$ = hiscoreMN$
ElseIf mode = 1
hiscoreThisMode = hiscore2M
hiscoreThisModeN$ = hiscore2MN$
ElseIf mode = 2
hiscoreThisMode = hiscore3M
hiscoreThisModeN$ = hiscore3MN$
EndIf
ElseIf Height = 25 ; big Size
If mode = 0
hiscoreThisMode = hiscoreB
hiscoreThisModeN$ = hiscoreBN$
ElseIf mode = 1
hiscoreThisMode = hiscore2B
hiscoreThisModeN$ = hiscore2BN$
ElseIf mode = 2
hiscoreThisMode = hiscore3B
hiscoreThisModeN$ = hiscore3BN$
EndIf
EndIf

Dim blocks.b(Width,Height)
For x = 0 To Width-1
For y = 0 To Height-1
Blocks(x,y) = 0
Next
Next
Gosub getNextPiece
Gosub setCurrentPiece
Gosub getNextPiece
level = 0
lines = 0
Gosub loadHiScore
score = 0
PieceX = Width/2
PieceY = 0
time = ElapsedMilliseconds()
Movetime = ElapsedMilliseconds()
Fliptime = ElapsedMilliseconds()
LevelSpeed = 350
TimeD = LevelSpeed
MoveD = 70
flipD = 150
playing = 1
If musicPlaying = 1
If IsSound(BackGround)
StopSound(BackGround)
PlaySound(BackGround,1)
EndIf
EndIf

Return




Return

;**********************************************************
;
;- loadHiScore
;
;**********************************************************
LoadHiScore:
    If ReadFile(0, "blocks.hs")
hiscore   = ReadLong(0)+1234567890
hiscore2  = ReadLong(0)+1234567890
hiscore3  = ReadLong(0)+1234567890
hiscoreM  = ReadLong(0)+1234567890
hiscore2M = ReadLong(0)+1234567890
hiscore3M = ReadLong(0)+1234567890
hiscoreB  = ReadLong(0)+1234567890
hiscore2B = ReadLong(0)+1234567890
hiscore3B = ReadLong(0)+1234567890

hiscoreN$   = ReadString(0)
hiscore2N$  = ReadString(0)
hiscore3N$  = ReadString(0)
hiscoreMN$   = ReadString(0)
hiscore2MN$ = ReadString(0)
hiscore3MN$ = ReadString(0)
hiscoreBN$   = ReadString(0)
hiscore2BN$ = ReadString(0)
hiscore3BN$ = ReadString(0)
CloseFile(0)
EndIf
Return

;**********************************************************
;
;- saveHiScore
;
;**********************************************************
SaveHiScore:
    If Height = 10 ; baby
If score > hiscore And mode = 0
hiscore = score
hiscoreN$ = getName()
ElseIf score > hiscore2 And mode = 1
hiscore2 = score
hiscore2N$ = getName()
ElseIf score > hiscore3 And mode = 2
hiscore3 = score
hiscore3N$ = getName()
EndIf
ElseIf Height = 20
If score > hiscoreM And mode = 0
hiscoreM = score
hiscoreMN$ = getName()
ElseIf score > hiscore2M And mode = 1
hiscore2M = score
hiscore2MN$ = getName()
ElseIf score > hiscore3M And mode = 2
hiscore3M = score
hiscore3MN$ = getName()
EndIf
ElseIf Height = 25
If score > hiscoreB And mode = 0
hiscoreB = score
hiscoreBN$ = getName()
ElseIf score > hiscore2B And mode = 1
hiscore2B = score
hiscore2BN$ = getName()
ElseIf score > hiscore3B And mode = 2
hiscore3B = score
hiscore3BN$ = getName()
EndIf
EndIf

If OpenFile(0, "blocks.hs")
WriteLong(0,hiscore-1234567890)  ; baby size
WriteLong(0,hiscore2-1234567890)
WriteLong(0,hiscore3-1234567890)

WriteLong(0,hiscoreM-1234567890)   ; Normal size
WriteLong(0,hiscore2M-1234567890)
WriteLong(0,hiscore3M-1234567890)

WriteLong(0,hiscoreB-1234567890)   ; Big Size
WriteLong(0,hiscore2B-1234567890)
WriteLong(0,hiscore3B-1234567890)

WriteStringN(0,hiscoreN$)  ; names
WriteStringN(0,hiscore2N$)
WriteStringN(0,hiscore3N$)

WriteStringN(0,hiscoreMN$)
WriteStringN(0,hiscore2MN$)
WriteStringN(0,hiscore3MN$)

WriteStringN(0,hiscoreBN$)
WriteStringN(0,hiscore2BN$)
WriteStringN(0,hiscore3BN$)
CloseFile(0)
EndIf
Return


;**********************************************************
;
;- GetNextPiece
;
;**********************************************************
GetNextPiece:

    If mode > 0 ; not baby
If mode = 1 ; NORMAL MODE
g = Random(6)
ElseIf mode = 2 ; BLOCKS MODE
g = Random(28)
EndIf

If g = 0
np.piece\image=#Block
np.piece\b1x=0   ; a square
np.piece\b1y=0
np.piece\b2x=1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=1
np.piece\b4x=1
np.piece\b4y=1  ; since these extra blocks arent used in this mode
np.piece\b5x=0  ; just make the piece over another piece to make only 4 appear.
np.piece\b5y=0
np.piece\b6x=1
np.piece\b6y=0
ElseIf g = 1
np.piece\image=#Block+1
np.piece\b1x=-1   ; L
np.piece\b1y=1
np.piece\b2x=-1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=0
np.piece\b5x=-1
np.piece\b5y=1
np.piece\b6x=-1
np.piece\b6y=0
ElseIf g = 2
np.piece\image=#Block+2
np.piece\b1x=-1   ; L-backwards
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=1
np.piece\b5x=0
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 3
np.piece\image=#Block+3
np.piece\b1x=-1   ; Line
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=2
np.piece\b4y=0
np.piece\b5x=-1
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 4
np.piece\image=#Block+4
np.piece\b1x=1   ; N
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=1
np.piece\b4x=-1
np.piece\b4y=1
np.piece\b5x=1
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 5
np.piece\image=#Block+5
np.piece\b1x=1   ; N-backwards
np.piece\b1y=1
np.piece\b2x=0
np.piece\b2y=1
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=-1
np.piece\b4y=0
np.piece\b5x=1
np.piece\b5y=1
np.piece\b6x=0
np.piece\b6y=1
ElseIf g = 6
np.piece\image=#Block+6
np.piece\b1x=0   ; T
np.piece\b1y=1
np.piece\b2x=-1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=0
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=-1
np.piece\b6y=0
ElseIf g = 7            ;********************* BLOCK ********************
np.piece\image=#Block+7
np.piece\b1x=0   ; dot
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=0
np.piece\b4y=0
np.piece\b5x=0
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 8
np.piece\image=#Block+8
np.piece\b1x=-1   ; Screw
np.piece\b1y=-1
np.piece\b2x=-1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=0
np.piece\b5x=1
np.piece\b5y=1
np.piece\b6x=-1
np.piece\b6y=-1
ElseIf g = 9
np.piece\image=#Block+9
np.piece\b1x=1    ; Screw backwards
np.piece\b1y=-1
np.piece\b2x=1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=-1
np.piece\b4y=0
np.piece\b5x=-1
np.piece\b5y=1
np.piece\b6x=1
np.piece\b6y=-1
ElseIf g = 10
np.piece\image=#Block+10
np.piece\b1x=-1   ; long cross
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=0
np.piece\b4y=-1
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 11
np.piece\image=#Block+11
np.piece\b1x=-1   ; cross
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=0
np.piece\b4y=-1
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=2
np.piece\b6y=0
ElseIf g = 12
np.piece\image=#Block+12
np.piece\b1x=-1   ; layers
np.piece\b1y=-1
np.piece\b2x=0
np.piece\b2y=-1
np.piece\b3x=1
np.piece\b3y=-1
np.piece\b4x=-1
np.piece\b4y=1
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=1
np.piece\b6y=1
ElseIf g = 13
np.piece\image=#Block+13
np.piece\b1x=-1   ; Y
np.piece\b1y=-1
np.piece\b2x=-1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=0
np.piece\b4y=1
np.piece\b5x=1
np.piece\b5y=-1
np.piece\b6x=1
np.piece\b6y=0
ElseIf g = 14
np.piece\image=#Block+14
np.piece\b1x=-1   ; U
np.piece\b1y=0
np.piece\b2x=-1
np.piece\b2y=1
np.piece\b3x=0
np.piece\b3y=1
np.piece\b4x=0
np.piece\b4y=1
np.piece\b5x=1
np.piece\b5y=0
np.piece\b6x=1
np.piece\b6y=1
ElseIf g = 15
np.piece\image=#Block+15
np.piece\b1x=-2   ; 5 line
np.piece\b1y=0
np.piece\b2x=-1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=0
np.piece\b5x=2
np.piece\b5y=0
np.piece\b6x=2
np.piece\b6y=0
ElseIf g = 16
np.piece\image=#Block+16
np.piece\b1x=-2   ; 6 line
np.piece\b1y=0
np.piece\b2x=-1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=0
np.piece\b5x=2
np.piece\b5y=0
np.piece\b6x=3
np.piece\b6y=0
ElseIf g = 17
np.piece\image=#Block+17
np.piece\b1x=-1   ; 3x2 block
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=-1
np.piece\b4y=1
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=1
np.piece\b6y=1
ElseIf g = 18
np.piece\image=#Block+18
np.piece\b1x=-1   ; zig-zag
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=1
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=-1
np.piece\b4y=0
np.piece\b5x=-1
np.piece\b5y=0
np.piece\b6x=-1
np.piece\b6y=0
ElseIf g = 19
np.piece\image=#Block+19
np.piece\b1x=-1   ; 2x2 block + notch top
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=-1
np.piece\b4y=1
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=1
np.piece\b6y=0
ElseIf g = 20
np.piece\image=#Block+20
np.piece\b1x=-1   ; 2x2 block + notch bottom
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=-1
np.piece\b4y=1
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=1
np.piece\b6y=1
ElseIf g = 21   ; ************* BLOCKS
np.piece\image=#Block+21
np.piece\b1x=0   ; Small L
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=1
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=0
np.piece\b4y=0
np.piece\b5x=0
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 22
np.piece\image=#Block+22
np.piece\b1x=-1   ; Big T
np.piece\b1y=-1
np.piece\b2x=0
np.piece\b2y=-1
np.piece\b3x=1
np.piece\b3y=-1
np.piece\b4x=0
np.piece\b4y=0
np.piece\b5x=0
np.piece\b5y=1
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 23
np.piece\image=#Block+23
np.piece\b1x=-1   ; Short parallel, by connie :)
np.piece\b1y=0
np.piece\b2x=-1
np.piece\b2y=1
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=1
np.piece\b5x=1
np.piece\b5y=1
np.piece\b6x=1
np.piece\b6y=1
ElseIf g = 24
np.piece\image=#Block+24
np.piece\b1x=-1   ; Big L backwards
np.piece\b1y=-1
np.piece\b2x=0
np.piece\b2y=-1
np.piece\b3x=1
np.piece\b3y=-1
np.piece\b4x=1
np.piece\b4y=0
np.piece\b5x=1
np.piece\b5y=1
np.piece\b6x=-1
np.piece\b6y=-1
ElseIf g = 25
np.piece\image=#Block+25
np.piece\b1x=-2   ; TO
np.piece\b1y=0
np.piece\b2x=-1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
np.piece\b4x=1
np.piece\b4y=0
np.piece\b5x=2
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=1
ElseIf g = 26
np.piece\image=#Block+26
np.piece\b1x=-1   ; Small L
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=1
np.piece\b4x=0
np.piece\b4y=0
np.piece\b5x=0
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 27
np.piece\image=#Block+27
np.piece\b1x=-1   ; 3 by 1 line
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=0
np.piece\b4x=0
np.piece\b4y=0
np.piece\b5x=0
np.piece\b5y=0
np.piece\b6x=0
np.piece\b6y=0
ElseIf g = 28
np.piece\image=#Block+28
np.piece\b1x=-1+Random(2)   ; crazy
np.piece\b1y=-1+Random(2)
np.piece\b2x=-1+Random(2)
np.piece\b2y=-1+Random(2)
np.piece\b3x=-1+Random(2)
np.piece\b3y=-1+Random(2)
np.piece\b4x=-1+Random(2)
np.piece\b4y=-1+Random(2)
np.piece\b5x=-1+Random(2)
np.piece\b5y=-1+Random(2)
np.piece\b6x=-1+Random(2)
np.piece\b6y=-1+Random(2)
EndIf
Else  ; BABY BLOCKS
g = Random(8)

If g = 0
np.piece\image=#Block
np.piece\b1x=0   ; a dot
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
ElseIf g = 1
np.piece\image=#Block+1
np.piece\b1x=0   ; line
np.piece\b1y=-1
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=1
ElseIf g = 2
np.piece\image=#Block+2
np.piece\b1x=-1   ; spaced
np.piece\b1y=0
np.piece\b2x=1
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=0
ElseIf g = 3
np.piece\image=#Block+3
np.piece\b1x=-1   ; L
np.piece\b1y=0
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=1
ElseIf g = 4
np.piece\image=#Block+4
np.piece\b1x=0   ; two line
np.piece\b1y=0
np.piece\b2x=1
np.piece\b2y=0
np.piece\b3x=0
np.piece\b3y=0
ElseIf g = 5
np.piece\image=#Block+5
np.piece\b1x=-1   ; Zig
np.piece\b1y=-1
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=1
np.piece\b3y=1
ElseIf g = 6
np.piece\image=#Block+6
np.piece\b1x=-1   ; bent
np.piece\b1y=-1
np.piece\b2x=0
np.piece\b2y=0
np.piece\b3x=-1
np.piece\b3y=1
ElseIf g = 7
np.piece\image=#Block+7
np.piece\b1x= -1+Random(1)
np.piece\b1y= -1+Random(1)
np.piece\b2x= -1+Random(1)
np.piece\b2y= -1+Random(1)
np.piece\b3x= -1+Random(1)
np.piece\b3y= -1+Random(1)
EndIf
np.piece\b4x = np.piece\b1x
np.piece\b4y = np.piece\b1y; since these extra blocks arent used in this mode
np.piece\b5x = np.piece\b1x; just make the piece over another piece to make only 3
np.piece\b5y = np.piece\b1y
np.piece\b6x = np.piece\b1x
np.piece\b6y = np.piece\b1y

EndIf

Return



Return
;**********************************************************
;
;- SetCurrentPiece
;
;**********************************************************
SetCurrentPiece:
    CurrentPiece = g
cp.piece\image = np.piece\image
cp.piece\b1x = np.piece\b1x
cp.piece\b1y = np.piece\b1y
cp.piece\b2x = np.piece\b2x
cp.piece\b2y = np.piece\b2y
cp.piece\b3x = np.piece\b3x
cp.piece\b3y = np.piece\b3y
cp.piece\b4x = np.piece\b4x
cp.piece\b4y = np.piece\b4y
cp.piece\b5x = np.piece\b5x
cp.piece\b5y = np.piece\b5y
cp.piece\b6x = np.piece\b6x
cp.piece\b6y = np.piece\b6y

Return

;**********************************************************
;
;- GameEnd
;
;**********************************************************
GameEnd:
    Gosub SaveHiScore
End   ; :D enjoy


; IDE Options = PureBasic 4.10 (Windows - x86)
; CursorPosition = 106
; FirstLine = 85
; Folding = -
; UseIcon = images\icon.ico
; Executable = Blocks.exe
