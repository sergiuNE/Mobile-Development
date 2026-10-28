# Skills installeren

Eenmalig per laptop. `/plugin`-commando's typ je in Claude Code, `npx`-commando's in een terminal (`-g` = voor al je projecten). Herstart Claude Code na installeren.

## Plugins (Claude Code)

```
/plugin install superpowers@claude-plugins-official
/plugin install figma@claude-plugins-official
```

`claude-plugins-official` is standaard beschikbaar. Na het installeren van `figma`: open `/plugin`, kies Figma en log in via de browser.

```
/plugin marketplace add anthropics/knowledge-work-plugins
/plugin install design@knowledge-work-plugins

/plugin marketplace add JuliusBrussee/caveman
/plugin install caveman@caveman

/plugin marketplace add DietrichGebert/ponytail
/plugin install ponytail@ponytail

/plugin marketplace add latent-spaces/brag
/plugin install brag@brag
```

## Losse skills

```bash
npx skills add pbakaus/impeccable -g
npx skills add jakubkrehel/make-interfaces-feel-better -g
npx skills add addyosmani/agent-skills --skill incremental-implementation -g
npx skills add mattpocock/skills --skill handoff -g
npx skills add addyosmani/agent-skills --skill security-and-hardening -g
npx skills add shadcn/ui -g
npx skills add woerndl/unsloppify -g
npx skills add Leonxlnx/taste-skill -g
npx skills add vercel-labs/agent-skills --skill web-design-guidelines -g
```

## Handmatig geïnstalleerd

Deze skills staan in `~/.claude/skills/`, maar niet in de lock-file van `npx skills`. De bron is nog onbekend: vul het installatiecommando aan of kopieer de map van een laptop waarop ze al staan.

- `animate`
- `animation-performance`
- `improve-animations`
- `review-animations`
- `emil-design-eng`
- `image-to-code`
- `gdpr-compliance`
- `playwright-cli`

## Ingebouwd (niks installeren)

`/code-review`, `/security-review`, `/init`

## Controleren

Typ `/` in Claude Code. De skills uit deze lijst moeten erin staan.
