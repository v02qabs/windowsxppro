bits 16
org 100h

global start

start:
	mov ah,09
	mov dx,Hello

	int 21h

	mov ah,4ch
	mov al,0
	int 21h

Hello db "文字列を入力してください。", 0dh,0ah, '$'
